package com.wgzhao.addax.plugin.writer.starrockswriter.manager;

import java.util.List;

public class StarRocksFlushTuple {

    private String label;
    private final Long bytes;
    private final List<byte[]> rows;

    public StarRocksFlushTuple(String label, Long bytes, List<byte[]> rows) {
        this.label = label;
        this.bytes = bytes;
        this.rows = rows;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Long getBytes() {
        return bytes;
    }

    public List<byte[]> getRows() {
        return rows;
    }
}
