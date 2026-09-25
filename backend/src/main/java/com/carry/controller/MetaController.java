package com.carry.controller;

import com.carry.entity.LocationArea;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/meta")
public class MetaController {

    @GetMapping("/zones")
    public ResponseEntity<List<LocationArea>> getZones() {
        return ResponseEntity.ok(Arrays.asList(LocationArea.values()));
    }
}
