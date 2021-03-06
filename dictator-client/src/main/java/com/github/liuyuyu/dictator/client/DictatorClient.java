package com.github.liuyuyu.dictator.client;

import com.github.liuyuyu.dictator.client.http.MediaTypeConstants;
import com.github.liuyuyu.dictator.common.ApiUrlConstants;
import com.github.liuyuyu.dictator.common.BaseProperties;
import com.github.liuyuyu.dictator.common.model.dto.DictatorValueResponse;
import com.github.liuyuyu.dictator.common.model.request.PropertyGetRequest;
import com.github.liuyuyu.dictator.common.model.response.DataWrapper;
import com.github.liuyuyu.dictator.common.utils.JsonUtils;
import lombok.*;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/*
 * @author liuyuyu
 */
@Slf4j
@Data
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class DictatorClient {
    /**
     * 默认客户端实现
     */
    @Setter(AccessLevel.PRIVATE)
    private OkHttpClient okHttpClient = new OkHttpClient.Builder()
            .build();

    private DictatorClientProperties dictatorClientProperties;

    public static DictatorClient of(@NonNull DictatorClientProperties dictatorClientProperties) {
        if (dictatorClientProperties.getAppId() == null) {
            throw new IllegalArgumentException("appId can not be null.");
        }
        if (dictatorClientProperties.getDeploymentId() == null) {
            throw new IllegalArgumentException("deploymentId can not be null.");
        }
        if (dictatorClientProperties.getServerUrl() == null) {
            throw new IllegalArgumentException("serverUrl can not be null.");
        }
        DictatorClient dictatorClient = new DictatorClient();
        dictatorClient.setDictatorClientProperties(dictatorClientProperties);
        return dictatorClient;
    }

    public String get(@NonNull String propertyName) {
        PropertyGetRequest propertyGetRequest = PropertyGetRequest.from(this.dictatorClientProperties);
        propertyGetRequest.setPropertyName(propertyName);
        Request request = new Request.Builder()
                .url(String.format("%s/%s", this.dictatorClientProperties.getServerUrl(), ApiUrlConstants.CONFIG_GET_URI))
                .post(RequestBody.create(MediaTypeConstants.APPLICATION_JSON_UTF8, JsonUtils.toJson(propertyGetRequest)))
                .build();
        try {
            Response response = this.okHttpClient.newCall(request).execute();
            if (response.code() == 200) {
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    String responseBodyString = responseBody.string();
                    log.debug("dictator server response:{}", responseBodyString);
                    DataWrapper dataWrapper = JsonUtils.toObject(responseBodyString, DataWrapper.class);
                    if (dataWrapper != null && dataWrapper.getData() != null) {
                        if (dataWrapper.getSuccess() != null && dataWrapper.getSuccess()) {
                            DictatorValueResponse dictatorValueResponse = JsonUtils.toObject(JsonUtils.toJson(dataWrapper.getData()), DictatorValueResponse.class);
                            if (dictatorValueResponse != null) {
                                return dictatorValueResponse.getValue();
                            }
                        }
                    }
                    log.warn("config '{}' not found.", propertyName);
                }
            }
        } catch (IOException e) {
            log.error("properties load fail", e.getMessage());
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    public Map<String, String> reload(Long lastUpdatedTime) {
        BaseProperties batchRequest = BaseProperties.from(this.dictatorClientProperties);
        batchRequest.setLastUpdatedTime(lastUpdatedTime);
        Request request = new Request.Builder()
                .url(String.format("%s/%s", this.dictatorClientProperties.getServerUrl(), ApiUrlConstants.CONFIG_BATCH_GET_URI))
                .post(RequestBody.create(MediaTypeConstants.APPLICATION_JSON_UTF8, JsonUtils.toJson(batchRequest)))
                .build();
        try {
            Response response = this.okHttpClient.newCall(request).execute();
            if (response.code() == 200) {
                ResponseBody responseBody = response.body();
                if (responseBody != null) {
                    String responseBodyString = responseBody.string();
                    log.debug("dictator server response:{}", responseBodyString);
                    DataWrapper dataWrapper = JsonUtils.toObject(responseBodyString, DataWrapper.class);
                    if (dataWrapper != null && dataWrapper.getData() != null) {
                        if (dataWrapper.getSuccess() != null && dataWrapper.getSuccess()) {
                            return (Map<String, String>) dataWrapper.getData();
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("properties load fail", e.getMessage());
        }
        return new HashMap<>();
    }
}
