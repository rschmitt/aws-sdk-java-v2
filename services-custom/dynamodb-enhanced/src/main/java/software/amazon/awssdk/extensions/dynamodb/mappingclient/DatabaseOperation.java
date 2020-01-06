/*
 * Copyright 2010-2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package software.amazon.awssdk.extensions.dynamodb.mappingclient;

import java.util.function.Function;

import software.amazon.awssdk.annotations.SdkPublicApi;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

/**
 * Interface for a single operation that can be executed against a mapped database. These operations do not operate
 * on a specific table or index, and may reference multiple tables and indexes (eg: batch operations). Conceptually an
 * operation maps 1:1 with an actual DynamoDb call.
 *
 * Typically a database operation will be executed by a {@link MappedDatabase} which expects a supplier:
 *
 * {@code mappedDatabase.execute(() -> databaseOperation);}
 **
 * @param <RequestT>  The type of the request object for the DynamoDb call in the low level {@link DynamoDbClient}.
 * @param <ResponseT> The type of the response object for the DynamoDb call in the low level {@link DynamoDbClient}.
 * @param <ResultT> The type of the mapped result object that will be returned by the execution of this operation.
 */
@SdkPublicApi
public interface DatabaseOperation<RequestT, ResponseT, ResultT> {
    /**
     * This method generates the request that needs to be sent to a low level {@link DynamoDbClient}.
     * @param mapperExtension A {@link MapperExtension} that may modify the request of this operation. A null value
     *                        here will result in no modifications.
     * @return A request that can be used as an argument to a {@link DynamoDbClient} call to perform the operation.
     */
    RequestT generateRequest(MapperExtension mapperExtension);

    /**
     * Provides a function for making the low level SDK call to DynamoDb.
     * @param dynamoDbClient A low level {@link DynamoDbClient} to make the call against.
     * @return A function that calls DynamoDb with a provided request object and returns the response object.
     */
    Function<RequestT, ResponseT> serviceCall(DynamoDbClient dynamoDbClient);

    /**
     * Takes the response object returned by the actual DynamoDb call and maps it into a higher level abstracted
     * result object.
     * @param response The response object returned by the DynamoDb call for this operation.
     * @param mapperExtension A {@link MapperExtension} that may modify the result of this operation. A null value
     *                        here will result in no modifications.
     * @return A high level result object as specified by the implementation of this operation.
     */
    ResultT transformResponse(ResponseT response, MapperExtension mapperExtension);

    /**
     * Default implementation of a complete execution of this operation. It performs three steps:
     * 1) Call generateRequest() to get the request object.
     * 2) Call getServiceCall() and call it using the request object generated in the previous step.
     * 3) Call transformResponse() to convert the response object returned in the previous step to a high level result.
     *
     * @param dynamoDbClient A {@link DynamoDbClient} to make the call against.
     * @param mapperExtension A {@link MapperExtension} that may modify the request or result of this operation. A
     *                        null value here will result in no modifications.
     * @return A high level result object as specified by the implementation of this operation.
     */
    default ResultT execute(DynamoDbClient dynamoDbClient, MapperExtension mapperExtension) {
        RequestT request = generateRequest(mapperExtension);
        ResponseT response = serviceCall(dynamoDbClient).apply(request);
        return transformResponse(response, mapperExtension);
    }
}
