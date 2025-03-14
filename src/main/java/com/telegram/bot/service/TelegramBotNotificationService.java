package com.telegram.bot.service;

import com.telegram.bot.config.TelegramBotCredentialProvider;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

@Component
@RequiredArgsConstructor
@Slf4j
public class TelegramBotNotificationService extends TelegramLongPollingBot {
    public static final String COMMAND_START = "/start";
    public static final String CALLBACK_DATA_BUTTON1 = "button1";
    public static final String CALLBACK_DATA_BUTTON2 = "button2";
    public static final String CALLBACK_DATA_MENU1_NEXT_BUTTON = "menu1_next";
    public static final String CALLBACK_DATA_MENU2_BACK_BUTTON = "menu2_back";
    public static final String BUTTON_TEXT1 = "Кнопка 1";
    public static final String BUTTON_TEXT2 = "Кнопка 2";
    public static final String BUTTON_TEXT_MENU1 = "Меню 1";
    public static final String BUTTON_TEXT_MENU2 = "Меню 2";
    public static final String BUTTON_TEXT_NEXT = "Далі";
    public static final String BUTTON_TEXT_BACK = "Назад";

    private final TelegramBotCredentialProvider telegramBotCredentialProvider;

    @Override
    public String getBotUsername() {
        return telegramBotCredentialProvider.getBotName();
    }

    @Override
    public String getBotToken() {
        return telegramBotCredentialProvider.getToken();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (update.hasMessage() && update.getMessage().hasText()) {
            String messageText = update.getMessage().getText();
            long chatId = update.getMessage().getChatId();

            handleTextMessage(messageText, chatId);
        } else if (update.hasCallbackQuery()) {
            String callbackData = update.getCallbackQuery().getData();
            long chatId = update.getCallbackQuery().getMessage().getChatId();

            handleCallbackData(callbackData, chatId);
        }
    }

    private void handleTextMessage(String messageText, long chatId) {
        log.info("Received text message: '{}' from chatId: {}", messageText, chatId);

        if (COMMAND_START.equals(messageText)) {
            sendMenu(chatId, BUTTON_TEXT_MENU1, buildMenu("menu1"));
        }
    }

    private void handleCallbackData(String callbackData, long chatId) {
        if (callbackData.startsWith(CALLBACK_DATA_BUTTON1)) {
            sendMessage(chatId, BUTTON_TEXT1, null);
        } else if (callbackData.startsWith(CALLBACK_DATA_BUTTON2)) {
            sendMessage(chatId, BUTTON_TEXT2, null);
        } else {
            switch (callbackData) {
                case CALLBACK_DATA_MENU1_NEXT_BUTTON -> sendMenu(chatId, BUTTON_TEXT_MENU2, buildMenu("menu2"));
                case CALLBACK_DATA_MENU2_BACK_BUTTON -> sendMenu(chatId, BUTTON_TEXT_MENU1, buildMenu("menu1"));
                default -> log.warn("Unknown callbackData: {}", callbackData);
            }
        }
    }

    private void sendMenu(long chatId, String menuText, List<List<InlineKeyboardButton>> keyboard) {
        InlineKeyboardMarkup keyboardMarkup = new InlineKeyboardMarkup();
        keyboardMarkup.setKeyboard(keyboard);
        sendMessage(chatId, menuText, keyboardMarkup);
    }

    private void sendMessage(long chatId, String text, InlineKeyboardMarkup keyboardMarkup) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId);
        message.setText(text);
        message.setReplyMarkup(keyboardMarkup);
        try {
            execute(message);
        } catch (TelegramApiException e) {
            log.error("Error sending message: {}", e.getMessage(), e);
        }
    }

    private List<List<InlineKeyboardButton>> buildMenu(String menuType) {
        List<List<InlineKeyboardButton>> keyboard = new ArrayList<>();

        List<InlineKeyboardButton> row1 = List.of(
                createButton(BUTTON_TEXT1, CALLBACK_DATA_BUTTON1 + "_" + menuType),
                createButton(BUTTON_TEXT2, CALLBACK_DATA_BUTTON2 + "_" + menuType)
        );

        List<InlineKeyboardButton> row2 = switch (menuType) {
            case "menu1" -> List.of(createButton(BUTTON_TEXT_NEXT, CALLBACK_DATA_MENU1_NEXT_BUTTON));
            case "menu2" -> List.of(createButton(BUTTON_TEXT_BACK, CALLBACK_DATA_MENU2_BACK_BUTTON));
            default -> throw new IllegalArgumentException("Unknown menu type: " + menuType);
        };

        keyboard.add(row1);
        keyboard.add(row2);
        return keyboard;
    }

    private InlineKeyboardButton createButton(String text, String callbackData) {
        InlineKeyboardButton button = new InlineKeyboardButton();
        button.setText(text);
        button.setCallbackData(callbackData);
        return button;
    }
}
