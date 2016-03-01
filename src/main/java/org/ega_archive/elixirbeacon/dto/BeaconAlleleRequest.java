package org.ega_archive.elixirbeacon.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Builder;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class BeaconAlleleRequest {
  
  // The bases that appear instead of the reference bases
  private String alternateBases;
  
  // Reference bases for this variant, starting from `position`
  private String referenceBases;
  
  // Chromosome identifier. Accepted values: 1-22, X, Y
  private String chromosome;

  // Position, allele locus (0-based)
  private Integer position;
  
  // Assembly identifier, e.g. `GRCh37
  private String referenceSet;
  
  // Identifiers of datasets, as defined in `BeaconDataset`. If this field is null/not specified, all datasets should be queried.
  private List<String> datasetIds;
  
}
