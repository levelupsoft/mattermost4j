/*
 * Copyright (c) 2017-present, Takayuki Maruyama
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package net.bis5.mattermost.client4;

import java.util.Map;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.apache.commons.lang3.StringUtils;
import lombok.AllArgsConstructor;
import net.bis5.mattermost.client4.model.ApiError;

/**
 * API response.
 * 
 * @author Takayuki Maruyama
 */
@AllArgsConstructor
public abstract class ApiResponse<T> {

  protected final Response response;

  public abstract T readEntity();

  public ApiError readError() {
    return response.readEntity(ApiError.class);
  }

  /**
   * If remote api returns error response, this method also return {@code false}.
   */
  public boolean hasError() {
    response.bufferEntity();
    try {
      readError();
      return true;
    } catch (ProcessingException ex) {
      return false;
    }
  }

  public Response getRawResponse() {
    return response;
  }

  public String getEtag() {
    return response.getHeaderString("Etag");
  }

  protected static final String STATUS = "status";
  protected static final String STATUS_OK = "ok";

  /**
   * a convenience function for checking the standard OK response from the web service.
   * 
   * @return The api response contains {@code true} when status OK, otherwise {@code false}.
   */
  public ApiResponse<Boolean> checkStatusOk() {
    Response rawResponse = getRawResponse();
    rawResponse.bufferEntity();
    if (rawResponse.getMediaType().isCompatible(MediaType.TEXT_PLAIN_TYPE)) {
      return checkPlainStatusOk(rawResponse);
    } else {
      return checkJsonStatusOk(rawResponse);
    }
  }


  protected ApiResponse<Boolean> checkPlainStatusOk(Response response) {
    String statusCode = response.readEntity(String.class);
    boolean success = StringUtils.equalsIgnoreCase(statusCode, STATUS_OK);
    return ApiResponse.of(response, success);
  }


  protected ApiResponse<Boolean> checkJsonStatusOk(Response response) {
    Map<String, String> m = response.readEntity(new GenericType<Map<String, String>>() {});
    boolean success = m != null && m.getOrDefault(STATUS, "").equalsIgnoreCase(STATUS_OK);
    return ApiResponse.of(response, success);
  }


  public static <T> ApiResponse<T> of(Response response, Class<T> entityClass) {
    return new EntityResponse<>(response, entityClass);
  }

  public static <T> ApiResponse<T> of(Response response, GenericType<T> genericType) {
    return new GenericResponse<>(response, genericType);
  }

  public static <T> ApiResponse<T> of(Response response, T data) {
    return new SimpleResponse<>(response, data);
  }

  private static class EntityResponse<T> extends ApiResponse<T> {
    private final Class<T> entityClass;

    public EntityResponse(Response response, Class<T> entityClass) {
      super(response);
      this.entityClass = entityClass;
    }

    @Override
    public T readEntity() {
      return response.readEntity(entityClass);
    }
  }

  private static class GenericResponse<T> extends ApiResponse<T> {
    private final GenericType<T> genericType;

    public GenericResponse(Response response, GenericType<T> genericType) {
      super(response);
      this.genericType = genericType;
    }

    @Override
    public T readEntity() {
      return response.readEntity(genericType);
    }
  }

  private static class SimpleResponse<T> extends ApiResponse<T> {
    private final T data;

    public SimpleResponse(Response response, T data) {
      super(response);
      this.data = data;
    }

    @Override
    public T readEntity() {
      return data;
    }
  }

}
