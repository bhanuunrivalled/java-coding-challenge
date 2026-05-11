package com.crewmeister.cmcodingchallenge.currency.api;

import com.crewmeister.cmcodingchallenge.currency.service.DeltaLoadResult;
import com.crewmeister.cmcodingchallenge.currency.service.FxLoadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/batch")
@Tag(name = "Batch Operations")
public class BatchController {

    private final FxLoadService fxLoadService;

    public BatchController(FxLoadService fxLoadService) {
        this.fxLoadService = fxLoadService;
    }

    @Operation(summary = "Trigger delta load manually")
    @PostMapping("/delta")
    public ResponseEntity<DeltaLoadResponse> triggerDelta() {
        try {
            DeltaLoadResult result = fxLoadService.executeDeltaLoad();
            return switch (result.status()) {
                case SUCCESS -> ResponseEntity.ok(
                        DeltaLoadResponse.success(result.startDate().toString(), result.endDate().toString()));
                case SKIPPED_EMPTY -> ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(DeltaLoadResponse.skippedEmpty());
                case SKIPPED_UP_TO_DATE -> ResponseEntity.ok(
                        DeltaLoadResponse.upToDate(result.reason()));
            };
        } catch (Exception ex) {
            // FIXME is it a good idea to thorw the trace to the UI in resposne ?
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(DeltaLoadResponse.error(ex.getMessage()));
        }
    }
}
