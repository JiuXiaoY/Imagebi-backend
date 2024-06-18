package com.image.project.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.image.project.model.entity.ChartMessage;
import com.image.project.service.ChartMessageService;
import com.image.project.mapper.ChartMessageMapper;
import org.springframework.stereotype.Service;

/**
* @author 86187
* @description 针对表【chart_message(消息通知表)】的数据库操作Service实现
* @createDate 2024-06-14 21:48:43
*/
@Service
public class ChartMessageServiceImpl extends ServiceImpl<ChartMessageMapper, ChartMessage>
    implements ChartMessageService{

}




