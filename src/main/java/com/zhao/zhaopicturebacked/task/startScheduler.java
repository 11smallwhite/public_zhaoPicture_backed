package com.zhao.zhaopicturebacked.task;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class startScheduler {
    @Value("${task.insertTask.isStart}")
    private String isStart;

    @PostConstruct
    public void init() {
        if(isStart.equals("1")){

            Thread thread = new Thread(new cleanPictureRunner());
            thread.start();
        }
    }



}
