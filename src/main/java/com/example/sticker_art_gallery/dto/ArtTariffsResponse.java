package com.example.sticker_art_gallery.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Тарифы начисления и списания ART")
public class ArtTariffsResponse {

    @Schema(description = "Правила начисления ART (credits)")
    private List<ArtTariffItem> credits;

    @Schema(description = "Правила списания ART (debits)")
    private List<ArtTariffItem> debits;

    public ArtTariffsResponse() {
    }

    public ArtTariffsResponse(List<ArtTariffItem> credits, List<ArtTariffItem> debits) {
        this.credits = credits;
        this.debits = debits;
    }

    public List<ArtTariffItem> getCredits() {
        return credits;
    }

    public void setCredits(List<ArtTariffItem> credits) {
        this.credits = credits;
    }

    public List<ArtTariffItem> getDebits() {
        return debits;
    }

    public void setDebits(List<ArtTariffItem> debits) {
        this.debits = debits;
    }
}
