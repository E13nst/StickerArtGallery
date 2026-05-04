# TON Pay Adapter

Небольшой Node-сервис для SDK `@ton-pay/api`.

## Env

- `TONPAY_CHAIN=testnet|mainnet`
- `TONPAY_API_KEY` - API key из TON Pay Merchant Dashboard
- `TONPAY_WEBHOOK_SECRET` - секрет подписи webhook
- `TONPAY_ADAPTER_TOKEN` - shared token для вызовов Java -> adapter
- `JAVA_TONPAY_WEBHOOK_URL` - например `https://api.example.com/api/internal/webhooks/tonpay`
- `JAVA_SERVICE_TOKEN` - `X-Service-Token` для Java internal API

## Endpoints

- `POST /api/tonpay/create-transfer`
- `GET /api/tonpay/status/reference/:reference`
- `GET /api/tonpay/status/body/:bodyBase64Hash`
- `POST /webhooks/tonpay`
- `GET /health`
