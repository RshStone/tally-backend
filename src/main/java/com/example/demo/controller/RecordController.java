package com.example.demo.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.demo.entity.Bill;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.demo.mapper.RecordMapper;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/record")
public class RecordController {
    @Resource
    private RecordMapper recordMapper;

    @GetMapping("/list")
    public List<Bill> list(@RequestParam(required = false) String category,
                           @RequestParam(required = false) String start,
                           @RequestParam(required = false) String end) {
        QueryWrapper<Bill> wrapper = new QueryWrapper<>();
        if (category != null && !category.isEmpty()) {
            wrapper.eq("category", category);
        }
        if (start != null && !start.isEmpty()) {
            wrapper.ge("record_date", start);
        }
        if (end != null && !end.isEmpty()) {
            wrapper.le("record_date", end);
        }
        wrapper.orderByDesc("record_date");
        return recordMapper.selectList(wrapper);
    }

    //按月统计收支: /api/record/stats?year=2026&month=10
    @GetMapping("{stats}")
    public List<Map<String, Object>> stats(@RequestParam int year,
                                           @RequestParam int month,
                                           @RequestParam(defaultValue = "1") long userId) {
        LocalDate first = LocalDate.of(year, month, 1);
        LocalDate next = first.plusMonths(1);
        QueryWrapper<Bill> wrapper = new QueryWrapper<>();
        wrapper.select("type", "SUM(amount) AS total")
                .eq("user_id", userId)
                .ge("record_date", first)
                .lt("record_date", next)
                .groupBy("type");
        return recordMapper.selectMaps(wrapper);
    }

    @GetMapping("/page")
    public IPage<Bill> page(@RequestParam(defaultValue = "1") int pageNum,
                            @RequestParam(defaultValue = "10") int pageSize) {
        Page<Bill> page = new Page<>(pageNum, pageSize);
        QueryWrapper<Bill> wrapper = new QueryWrapper<>();
        wrapper.orderByDesc("record_date").orderByDesc("id");;          // 分页也能配排序
        return recordMapper.selectPage(page, wrapper);
    }

    @PostMapping
    public Bill add(@RequestBody Bill bill){
        recordMapper.insert(bill);
        return bill;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id){
        recordMapper.deleteById(id);
    }

    @PutMapping
    public Bill update(@RequestBody Bill bill){
        recordMapper.updateById(bill);
        return bill;
    }
}
