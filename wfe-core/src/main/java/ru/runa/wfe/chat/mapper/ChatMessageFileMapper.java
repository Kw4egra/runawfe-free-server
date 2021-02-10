package ru.runa.wfe.chat.mapper;

import java.util.ArrayList;
import java.util.List;
import ru.runa.wfe.chat.ChatMessageFile;
import ru.runa.wfe.chat.dto.ChatMessageFileDetailDto;
import ru.runa.wfe.chat.dto.ChatMessageFileDto;

/**
 * @author Sergey Inyakin
 */
public class ChatMessageFileMapper implements ModelMapper<ChatMessageFile, ChatMessageFileDto> {
    @Override
    public ChatMessageFile toEntity(ChatMessageFileDto dto) {
        ChatMessageFile result = new ChatMessageFile();
        result.setId(dto.getId());
        result.setName(dto.getName());
        return result;
    }

    @Override
    public ChatMessageFileDto toDto(ChatMessageFile entity) {
        ChatMessageFileDto result = new ChatMessageFileDto();
        result.setId(entity.getId());
        result.setName(entity.getName());
        return result;
    }

    @Override
    public List<ChatMessageFileDto> toDto(List<ChatMessageFile> entities) {
        List<ChatMessageFileDto> result = new ArrayList<>(entities.size());
        for (ChatMessageFile file : entities) {
            result.add(toDto(file));
        }
        return result;
    }

    public ChatMessageFileDetailDto toDetailDto(ChatMessageFile entity) {
        ChatMessageFileDetailDto result = new ChatMessageFileDetailDto();
        result.setId(entity.getId());
        result.setName(entity.getName());
        return result;
    }

    public List<ChatMessageFileDetailDto> toDetailDto(List<ChatMessageFile> entities) {
        List<ChatMessageFileDetailDto> result = new ArrayList<>(entities.size());
        for (ChatMessageFile file : entities) {
            result.add(toDetailDto(file));
        }
        return result;
    }
}
