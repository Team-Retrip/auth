package com.retrip.auth.infra.adapter.out.external;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.retrip.auth.application.dto.CertificationInfo;
import com.retrip.auth.domain.exception.PortOneApiException;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class PortOneApiClient {

    private final OkHttpClient httpClient = new OkHttpClient();

    @Value("${portone.api_secret}")
    private String apiSecret;

    public CertificationInfo getCertificationInfo(String impUid) {
        String url = "https://api.portone.io/identity-verifications/" + impUid;

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Authorization", "PortOne " + apiSecret)
                .get()
                .build();

        try (Response response = httpClient.newCall(request).execute()) {
            String body = response.body().string();
            if (!response.isSuccessful()) {
                throw new PortOneApiException("본인인증 조회 실패: " + response.code());
            }

            JsonObject json = JsonParser.parseString(body).getAsJsonObject();
            if (!json.has("verifiedCustomer")) {
                throw new PortOneApiException("Missing verifiedCustomer in response");
            }

            JsonObject vc = json.getAsJsonObject("verifiedCustomer");
            String birthday = vc.has("birthDate")
                    ? getStringField(vc, "birthDate")
                    : getStringField(vc, "birthday");

            return CertificationInfo.builder()
                    .name(getStringField(vc, "name"))
                    .gender(getStringField(vc, "gender"))
                    .birthday(birthday)
                    .uniqueKey(getOptionalStringField(vc, "ci"))
                    .uniqueInSite(getOptionalStringField(vc, "di"))
                    .build();
        } catch (IOException e) {
            throw new PortOneApiException("IO Error: " + e.getMessage());
        }
    }

    private String getStringField(JsonObject json, String field) {
        if (!json.has(field) || json.get(field).isJsonNull()) {
            throw new PortOneApiException("Missing required field: " + field);
        }
        return json.get(field).getAsString();
    }

    private String getOptionalStringField(JsonObject json, String field) {
        if (!json.has(field) || json.get(field).isJsonNull()) return null;
        String v = json.get(field).getAsString();
        return v.isEmpty() ? null : v;
    }
}
