package com.jasonlat.ai.domain.agent.model.valobj;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author jasonlat
 * 2026-03-31  20:56
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiAgentRegisterVO {
    private String agentName;
}
