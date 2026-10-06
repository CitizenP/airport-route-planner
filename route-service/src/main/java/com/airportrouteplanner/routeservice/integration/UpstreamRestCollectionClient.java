package com.airportrouteplanner.routeservice.integration;

import java.util.List;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
class UpstreamRestCollectionClient {

    <T> List<T> getCollection(
            String serviceName,
            String baseUrl,
            String path,
            ParameterizedTypeReference<List<T>> responseType) {
        try {
            List<T> response = RestClient.create(baseUrl).get()
                    .uri(path)
                    .retrieve()
                    .body(responseType);
            if (response == null) {
                throw new UpstreamServiceException(serviceName + " returned no data from " + path);
            }
            return List.copyOf(response);
        } catch (UpstreamServiceException exception) {
            throw exception;
        } catch (RestClientException exception) {
            throw new UpstreamServiceException(
                    "could not retrieve " + path + " from " + serviceName,
                    exception);
        } catch (RuntimeException exception) {
            throw new UpstreamServiceException(
                    "could not read " + path + " from " + serviceName,
                    exception);
        }
    }
}
