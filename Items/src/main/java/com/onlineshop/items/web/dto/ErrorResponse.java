package com.onlineshop.items.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Error response following RFC 7807 (Problem Details for HTTP APIs)
 * https://www.rfc-editor.org/rfc/rfc7807
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    /**
     * A URI identifying the problem type.
     * Example: "https://api.example.com/errors/item-not-found"
     */
    private String type;

    /**
     * A brief, human-readable summary of the problem.
     * Should not change between problem occurrences.
     * Example: "Not Found"
     */
    private String title;

    /**
     * The HTTP status code.
     * Must match the status code in the HTTP response.
     */
    private int status;

    /**
     * A human-readable explanation specific to this problem occurrence.
     * Focuses on helping the client correct the problem.
     * Example: "Item not found with id: 999"
     */
    private String detail;

    /**
     * A URI identifying this specific problem occurrence.
     * Example: "/api/v1/items/999"
     */
    private String instance;
}
