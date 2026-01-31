package com.example.sticker_art_gallery.dto.referral;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Реферальная ссылка пользователя")
public class ReferralLinkDto {

    @Schema(description = "Уникальный реферальный код пользователя", example = "AbC123XyZ456")
    private String code;

    @Schema(description = "Параметр для deep-link (ref_<code>)", example = "ref_AbC123XyZ456")
    private String startParam;

    @Schema(description = "Полная ссылка для приглашения друзей", example = "https://t.me/stixlybot?startapp=ref_AbC123XyZ456")
    private String url;

    public ReferralLinkDto() {}

    public ReferralLinkDto(String code, String startParam, String url) {
        this.code = code;
        this.startParam = startParam;
        this.url = url;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getStartParam() {
        return startParam;
    }

    public void setStartParam(String startParam) {
        this.startParam = startParam;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }
}
