package com.bank;

import com.bank.console.ConsoleApp;

import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

/**
 * Точка входа в приложение.
 * Main отвечает только за запуск консольного интерфейса — вся бизнес-логика
 * находится в доменных (com.bank.domain) и сервисных (com.bank.service) классах.
 */
public class Main {
    public static void main(String[] args) {
        // Явно фиксируем UTF-8 для вывода: кодировка консоли по умолчанию
        // отличается от системы к системе (особенно на Windows), а в проекте
        // используется кириллица — без этого текст может отображаться "кракозябрами".
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        new ConsoleApp().run();
    }
}
