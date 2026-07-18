package com.retail.shift.dto;

import lombok.*;
import java.time.LocalTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShiftTypeRequest {
    private String shiftName;
    private LocalTime startTime;
    private LocalTime endTime;
}
