package com.restaurant.menuassistant.api.controller;

import com.restaurant.menuassistant.api.dto.ErrorResponse;
import com.restaurant.menuassistant.api.dto.MenuAnswerResponse;
import com.restaurant.menuassistant.api.dto.MenuQuestionRequest;
import com.restaurant.menuassistant.rag.RagService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/menu")
@Tag(name = "Menu Assistant", description = "AI-powered menu recommendation endpoint")
public class MenuController {

    private static final Logger log = LoggerFactory.getLogger(MenuController.class);

    private final RagService ragService;

    public MenuController(RagService ragService) {
        this.ragService = ragService;
    }

    @PostMapping("/ask")
    @Operation(
            summary = "Ask a menu question",
            description = "Submits a natural language question. The system retrieves the top-3 most relevant menu items via vector search and uses an LLM to generate a contextual recommendation.",
            responses = {
                    @ApiResponse(responseCode = "200", description = "Recommendation generated successfully"),
                    @ApiResponse(responseCode = "400", description = "Question is blank or null",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
                    @ApiResponse(responseCode = "503", description = "OpenAI API unavailable",
                            content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
            }
    )
    public ResponseEntity<MenuAnswerResponse> ask(@Valid @RequestBody MenuQuestionRequest request) {
        log.info("POST /api/menu/ask — question: '{}'", request.question());
        MenuAnswerResponse response = ragService.ask(request.question());
        return ResponseEntity.ok(response);
    }
}
