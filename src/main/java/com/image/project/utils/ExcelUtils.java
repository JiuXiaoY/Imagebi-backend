package com.image.project.utils;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.StrUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.support.ExcelTypeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ResourceUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author JXY
 * @version 1.0
 * @since 2024/6/4
 */
@Slf4j
public class ExcelUtils {
    public static String excelToCsv(MultipartFile multipartFile) {
        // 本地测试
        // File file = null;
        // try {
        //     file = ResourceUtils.getFile("classpath:user.xlsx");
        // } catch (FileNotFoundException e) {
        //     throw new RuntimeException(e);
        // }
        List<Map<Integer, String>> list = null;
        try {
            list = EasyExcel.read(multipartFile.getInputStream())
                    .excelType(ExcelTypeEnum.XLSX)
                    .sheet()
                    .headRowNumber(0)
                    .doReadSync();
        } catch (IOException e) {
            log.error("error: " + e.getMessage());
        }

        if (CollectionUtil.isEmpty(list)) {
            return "";
        }

        StringBuilder stringBuilder = new StringBuilder();

        LinkedHashMap<Integer, String> headerMap = (LinkedHashMap<Integer, String>) list.get(0);
        // 过滤掉 null
        List<String> headerList = headerMap.values().stream().filter(ObjectUtil::isNotEmpty).collect(Collectors.toList());
        stringBuilder.append(StrUtil.join(",", headerList)).append("\n");

        // 读取数据
        for (int i = 1; i < list.size(); i++) {
            LinkedHashMap<Integer, String> dataMap = (LinkedHashMap<Integer, String>) list.get(i);
            List<String> dataList = dataMap.values().stream().filter(ObjectUtil::isNotEmpty).collect(Collectors.toList());
            stringBuilder.append(StrUtil.join(",", dataList)).append("\n");
        }
        return stringBuilder.toString();
    }
}
