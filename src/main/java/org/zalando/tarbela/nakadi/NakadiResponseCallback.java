package org.zalando.tarbela.nakadi;

import java.util.List;

import org.springframework.http.HttpStatus;

import org.zalando.tarbela.nakadi.models.BatchItemResponse;
import org.zalando.tarbela.nakadi.models.Problem;

/**
 * A callback interface for the Nakadi submission responses. One of its methods will be called after a call to
 * {@link NakadiClient#submitEvents}.
 */
public interface NakadiResponseCallback {

    /**
     * All items were successfully submitted. (Status 200.)
     */
    void successfullyPublished();

    /**
     * A 207 response was returned, i.e. some of the items couldn't be published. Included is the list of all item
     * statuses, in the same order as in the request.
     */
    void partiallySubmitted(List<BatchItemResponse> responseList);

    /**
     * A 422 response was returned, i.e. some validation failed. Included is the list of all item statuses, in the same
     * order as in the request.
     */
    void validationProblem(List<BatchItemResponse> responseList);

    /**
     * Some unexpected success status was returned (neither 200 nor 207).
     */
    void otherSuccessStatus(HttpStatus status);

    /**
     * A client error (other than 422) did occur.
     *
     * @param  problem  a problem object describing the problem.
     */
    void clientError(Problem problem);

    /**
     * A server error did occur.
     *
     * @param  problem  a Problem object describing the problem.
     */
    void serverError(Problem problem);
}
