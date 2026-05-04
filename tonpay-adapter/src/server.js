import express from "express";
import crypto from "node:crypto";
import {
  TON,
  createTonPayTransfer,
  getTonPayTransferByBodyHash,
  getTonPayTransferByReference,
  verifySignature
} from "@ton-pay/api";

const app = express();
const port = Number(process.env.PORT || process.env.TONPAY_ADAPTER_PORT || 8787);

const config = {
  chain: process.env.TONPAY_CHAIN || "testnet",
  apiKey: process.env.TONPAY_API_KEY || undefined,
  apiSecret: process.env.TONPAY_WEBHOOK_SECRET || "",
  adapterToken: process.env.TONPAY_ADAPTER_TOKEN || "",
  javaWebhookUrl: process.env.JAVA_TONPAY_WEBHOOK_URL || "",
  javaServiceToken: process.env.JAVA_SERVICE_TOKEN || process.env.SERVICE_API_TOKEN || ""
};

function requireAdapterToken(req, res, next) {
  if (!config.adapterToken) {
    return next();
  }
  const provided = req.header("x-service-token") || "";
  const expected = Buffer.from(config.adapterToken);
  const actual = Buffer.from(provided);
  if (expected.length !== actual.length || !crypto.timingSafeEqual(expected, actual)) {
    return res.status(401).json({ error: "Unauthorized" });
  }
  next();
}

function nanoToTon(amountNano) {
  const value = BigInt(amountNano);
  return Number(value) / 1_000_000_000;
}

app.post("/api/tonpay/create-transfer", express.json(), requireAdapterToken, async (req, res) => {
  try {
    const {
      intentId,
      amountNano,
      asset = TON,
      recipientAddr,
      senderAddr,
      commentToSender,
      commentToRecipient
    } = req.body || {};

    if (!amountNano || !recipientAddr || !senderAddr) {
      return res.status(400).json({ error: "amountNano, recipientAddr and senderAddr are required" });
    }

    const transfer = await createTonPayTransfer(
      {
        amount: nanoToTon(amountNano),
        asset,
        recipientAddr,
        senderAddr,
        commentToSender: commentToSender || `ART ${intentId}`,
        commentToRecipient: commentToRecipient || `ART order ${intentId}`
      },
      {
        chain: config.chain,
        apiKey: config.apiKey
      }
    );

    res.json({
      message: transfer.message,
      reference: transfer.reference,
      bodyBase64Hash: transfer.bodyBase64Hash
    });
  } catch (error) {
    console.error("Failed to create TON Pay transfer", error);
    res.status(502).json({ error: "TON Pay transfer creation failed" });
  }
});

app.get("/api/tonpay/status/reference/:reference", requireAdapterToken, async (req, res) => {
  try {
    const info = await getTonPayTransferByReference(req.params.reference, {
      chain: config.chain,
      apiKey: config.apiKey
    });
    res.json(info);
  } catch (error) {
    console.error("Failed to lookup TON Pay reference", error);
    res.status(502).json({ error: "TON Pay status lookup failed" });
  }
});

app.get("/api/tonpay/status/body/:bodyBase64Hash", requireAdapterToken, async (req, res) => {
  try {
    const info = await getTonPayTransferByBodyHash(req.params.bodyBase64Hash, {
      chain: config.chain,
      apiKey: config.apiKey
    });
    res.json(info);
  } catch (error) {
    console.error("Failed to lookup TON Pay body hash", error);
    res.status(502).json({ error: "TON Pay status lookup failed" });
  }
});

app.post(
  "/webhooks/tonpay",
  express.raw({ type: "application/json" }),
  async (req, res) => {
    const rawBody = req.body.toString("utf8");
    const signature = req.header("x-tonpay-signature") || "";

    if (config.apiSecret) {
      const valid = verifySignature(rawBody, signature, config.apiSecret);
      if (!valid) {
        return res.status(401).json({ error: "Invalid signature" });
      }
    }

    if (!config.javaWebhookUrl) {
      console.warn("JAVA_TONPAY_WEBHOOK_URL is not configured; webhook accepted but not forwarded");
      return res.status(202).json({ received: true, forwarded: false });
    }

    const forward = await fetch(config.javaWebhookUrl, {
      method: "POST",
      headers: {
        "content-type": "application/json",
        "x-service-token": config.javaServiceToken,
        "x-tonpay-signature": signature
      },
      body: rawBody
    });

    if (!forward.ok) {
      const body = await forward.text();
      console.error("Java TON Pay webhook rejected", forward.status, body);
      return res.status(502).json({ error: "Java webhook rejected" });
    }

    res.json({ received: true, forwarded: true });
  }
);

app.get("/health", (_req, res) => {
  res.json({ ok: true, chain: config.chain });
});

app.listen(port, () => {
  console.log(`TON Pay adapter listening on :${port} (${config.chain})`);
});
