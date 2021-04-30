package ru.runa.wfe.chat.logic;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import net.bull.javamelody.MonitoredWithSpring;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import ru.runa.wfe.chat.ChatMessage;
import ru.runa.wfe.chat.ChatMessageFile;
import ru.runa.wfe.chat.ChatRoom;
import ru.runa.wfe.chat.ChatRoomClassPresentation;
import ru.runa.wfe.chat.dao.ChatFileIo;
import ru.runa.wfe.chat.dao.ChatMessageDao;
import ru.runa.wfe.chat.dto.ChatMessageFileDto;
import ru.runa.wfe.chat.dto.WfChatRoom;
import ru.runa.wfe.chat.dto.broadcast.MessageAddedBroadcast;
import ru.runa.wfe.chat.mapper.ChatMessageFileDetailMapper;
import ru.runa.wfe.chat.mapper.MessageAddedBroadcastFileMapper;
import ru.runa.wfe.chat.mapper.MessageAddedBroadcastMapper;
import ru.runa.wfe.commons.ClassLoaderUtil;
import ru.runa.wfe.commons.SystemProperties;
import ru.runa.wfe.commons.logic.WfCommonLogic;
import ru.runa.wfe.execution.Process;
import ru.runa.wfe.execution.logic.ExecutionLogic;
import ru.runa.wfe.presentation.BatchPresentation;
import ru.runa.wfe.security.AuthorizationException;
import ru.runa.wfe.security.Permission;
import ru.runa.wfe.security.SecuredObjectType;
import ru.runa.wfe.user.Actor;
import ru.runa.wfe.user.User;
import ru.runa.wfe.var.Variable;

@MonitoredWithSpring
public class ChatLogic extends WfCommonLogic {
    @Autowired
    private ExecutionLogic executionLogic;
    @Autowired
    private ChatMessageDao messageDao;
    @Autowired
    private MessageAddedBroadcastMapper messageMapper;
    @Autowired
    private MessageAddedBroadcastFileMapper messageFileMapper;
    @Autowired
    private ChatMessageFileDetailMapper fileDetailMapper;
    @Autowired
    private ChatFileIo fileIo;
    @Autowired
    private MessageTransactionWrapper messageTransactionWrapper;

    public MessageAddedBroadcast saveMessage(User user, Long processId, ChatMessage message, Set<Actor> recipients) {
        final ChatMessage savedMessage = messageTransactionWrapper.save(message, recipients, processId);
        return messageMapper.toDto(savedMessage);
    }

    public MessageAddedBroadcast saveMessage(User user, Long processId, ChatMessage message, Set<Actor> recipients, List<ChatMessageFileDto> files) {
        final List<ChatMessageFile> savedFiles = fileIo.save(files);
        try {
            final ChatMessage savedMessage = messageTransactionWrapper.save(message, recipients, savedFiles, processId);
            final MessageAddedBroadcast broadcast = messageMapper.toDto(savedMessage);
            broadcast.setFiles(fileDetailMapper.toDtos(savedFiles));
            return broadcast;
        } catch (Exception exception) {
            fileIo.delete(savedFiles);
            throw exception;
        }
    }

    public List<Long> getRecipientIdsByMessageId(User user, Long messageId) {
        return messageDao.getRecipientIdsByMessageId(messageId);
    }

    public ChatMessage getMessageById(User user, Long messageId) {
        return messageDao.get(messageId);
    }

    @Transactional
    public List<MessageAddedBroadcast> getMessages(User user, Long processId) {
        List<ChatMessage> messages = messageDao.getMessages(user.getActor(), processId);
        if (!messages.isEmpty()) {
            for (List<ChatMessage> messagesPart : Lists.partition(messages, SystemProperties.getDatabaseParametersCount())) {
                messageDao.readMessages(user.getActor(), messagesPart);
            }
        }
        return messageFileMapper.toDtos(messages);
    }

    public Long getNewMessagesCount(User user) {
        return messageDao.getNewMessagesCount(user.getActor());
    }

    public void deleteMessage(User user, Long messageId) {
        fileIo.delete(messageTransactionWrapper.delete(user, messageId));
    }

    public void updateMessage(User user, ChatMessage message) {
        if (!message.getCreateActor().equals(user.getActor())) {
            throw new AuthorizationException("Allowed for author only");
        }
        messageDao.update(message);
    }

    public void deleteMessages(Actor actor, Long processId) {
        messageDao.deleteMessages(processId);
    }

    @Transactional(readOnly = true)
    public int getChatRoomsCount(User user, BatchPresentation batchPresentation) {
        batchPresentation.getType().getRestrictions().add(ChatRoomClassPresentation.getExecutorIdRestriction(user.getActor().getId()));
        int count = getPersistentObjectCount(user, batchPresentation, Permission.READ, new SecuredObjectType[]{SecuredObjectType.PROCESS});
        batchPresentation.getType().getRestrictions().remove(ChatRoomClassPresentation.getExecutorIdRestriction(user.getActor().getId()));
        return count;
    }

    @Transactional(readOnly = true)
    public List<WfChatRoom> getChatRooms(User user, BatchPresentation batchPresentation) {
        batchPresentation.getType().getRestrictions().add(ChatRoomClassPresentation.getExecutorIdRestriction(user.getActor().getId()));
        List<ChatRoom> chatRooms = getPersistentObjects(user, batchPresentation, Permission.READ,
                new SecuredObjectType[]{SecuredObjectType.PROCESS}, true);
        batchPresentation.getType().getRestrictions().remove(ChatRoomClassPresentation.getExecutorIdRestriction(user.getActor().getId()));
        return toWfChatRooms(chatRooms, batchPresentation.getDynamicFieldsToDisplay(true));
    }

    private List<WfChatRoom> toWfChatRooms(List<ChatRoom> chatRooms, List<String> variableNamesToInclude) {
        Map<Process, Map<String, Variable<?>>> variables = getVariables(chatRooms, variableNamesToInclude);
        List<WfChatRoom> wfChatRooms = Lists.newArrayListWithExpectedSize(chatRooms.size());
        for (ChatRoom room : chatRooms) {
            Process process = room.getProcess();
            WfChatRoom wfChatRoom = new WfChatRoom(process, executionLogic.getProcessErrors(process), room.getNewMessagesCount());
            wfChatRoom.getProcess().addAllVariables(executionLogic.getVariables(variableNamesToInclude, variables, process));
            wfChatRooms.add(wfChatRoom);
        }
        return wfChatRooms;
    }

    private Map<Process, Map<String, Variable<?>>> getVariables(List<ChatRoom> chatRooms, List<String> variableNamesToInclude) {
        Set<Process> processes = Sets.newHashSetWithExpectedSize(chatRooms.size());
        for (ChatRoom room : chatRooms) {
            processes.add(room.getProcess());
        }
        return variableDao.getVariables(processes, variableNamesToInclude);
    }
}
