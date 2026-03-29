package com.jasonlat.ai.types.page;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

import java.util.List;

@Data
public class PageVO<T> {
    List<T> list;
    long total;
    long pages;

    public PageVO(Page<T> page) {
        this.list = page.getRecords();
        this.total = page.getTotal();
        this.pages = page.getPages();
    }

    public PageVO(List<T> list, long total, long pages) {
        this.list = list;
        this.total = total;
        this.pages = pages;
    }
}
