package org.ega_archive.elixirbeacon.dto;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

@JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataUseCondition {
  
  protected DataUseHeader header;
  
}
