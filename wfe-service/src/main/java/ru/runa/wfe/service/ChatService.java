package ru.runa.wfe.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import ru.runa.wfe.chat.ChatMessage;
import ru.runa.wfe.chat.dto.ChatMessageFileDto;
import ru.runa.wfe.chat.dto.broadcast.MessageAddedBroadcast;
import ru.runa.wfe.user.Actor;
import ru.runa.wfe.user.User;

/**
 * Chat service.
 *
 * @author mrumyantsev
 * @since 02.02.2020
 */
public interface ChatService {

    public List<Long> getMentionedExecutorIds(User user, Long messageId);

    public void deleteFile(User user, Long id);

    public MessageAddedBroadcast saveMessageAndBindFiles(User user, Long processId, ChatMessage message, Set<Actor> recipients,
            ArrayList<ChatMessageFileDto> files);

    public void readMessage(User user, Long messageId);

    public Long getLastReadMessage(User user, Long processId);

    public Long getLastMessage(User user, Long processId);

    public List<Long> getActiveChatIds(User user);

    public List<Long> getNewMessagesCounts(User user, List<Long> processIds);

    /**
     * merge message in DB
     *
     * @param message message to merge
     */
    public void updateChatMessage(User user, ChatMessage message);

    /**
     * Get List array of all ChatMessageFiles in chat message.
     *
     * @param message chat message associated files
     * @return not <code>null</code>
     */
    public List<ChatMessageFileDto> getChatMessageFiles(User user, ChatMessage message);

    /**
     * Get ChatMessageFiles by id.
     *
     * @param fileId file Id
     * @return ChatMessageFiles or <code>null</code>
     */
    public ChatMessageFileDto getChatMessageFile(User user, Long fileId);

    /**
     * Save ChatMessageFiles.
     *
     * @param file new file to save (associated message in ChatMessageFiles)
     * @return not <code>null</code>
     */
    public ChatMessageFileDto saveChatMessageFile(User user, ChatMessageFileDto file);

    /**
     * Gets ChatMessage.
     *
     * @param messageId message Id
     * @return ChatMessage or <code>null</code>
     */
    public ChatMessage getChatMessage(User user, Long messageId);

    /**
     * Get List array of ChatMessage, where all "message Id" < firstId.
     *
     * @param processId chat Id
     * @param firstId   message Id, all returned message id < firstId
     * @param count     number of messages in the returned array
     * @return not <code>null</code> order by date desc
     */
    public List<MessageAddedBroadcast> getChatMessages(User user, Long processId, Long firstId, int count);

    /**
     * Get List array of ChatMessage, where all "message Id" >= lastId.
     *
     * @param processId chat Id
     * @param lastId    message Id, all returned message id >= lastId
     * @return not <code>null</code> order by date asc
     */
    public List<MessageAddedBroadcast> getNewChatMessages(User user, Long processId);

    /**
     * Save ChatMessage in DB.
     *
     * @param user
     * @param processId  chat Id
     * @param message    new message to save
     * @param recipients
     * @return new message id
     */
    public Long saveChatMessage(User user, Long processId, ChatMessage message, Set<Actor> recipients);

    /**
     * Delete ChatMessage in DB.
     *
     * @param messId message Id
     */
    public void deleteChatMessage(User user, Long messId);

    /**
     * Get number of chat messages with id > lastMessageId.
     *
     * @param processId     chat Id
     * @param lastMessageId last message Id
     * @return number of chat messages with id > lastMessageId
     */
    public Long getNewChatMessagesCount(User user, Long processId);
}
