package com.linan.system.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.linan.system.entity.TodoItem;
import com.linan.system.mapper.TodoItemMapper;
import com.linan.system.service.ITodoItemService;
import org.springframework.stereotype.Service;

@Service
public class TodoItemServiceImpl extends ServiceImpl<TodoItemMapper, TodoItem> implements ITodoItemService {
}
