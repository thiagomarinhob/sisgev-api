CREATE INDEX idx_evidences_confirmed_segment
    ON inspection_evidences(confirmed_road_segment_id)
    WHERE confirmed_road_segment_id IS NOT NULL;
