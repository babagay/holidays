import React, { useState, useRef, useEffect } from 'react';
import { useConfig } from '../Context/Config.js';

const EnhancedStreamTest = () => {
    const [response, setResponse] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [chunkCount, setChunkCount] = useState(0);
    const [totalChars, setTotalChars] = useState(0);
    const [error, setError] = useState('');

    const { apiUrl } = useConfig();
    const base = 'http://localhost:8080';
    const streamUrl = `${base}/chat/stream/test-flux`;

    const abortControllerRef = useRef(null);
    const responseEndRef = useRef(null);

    // Автопрокрутка
//    useEffect(() => {
//        if (responseEndRef.current) {
//            responseEndRef.current.scrollIntoView({ behavior: 'smooth' });
//        }
//    }, [response]);

    // Тестовые запросы
    const testRequests = [
        {
            name: "🏔️ Горы Болгарии",
            prompt: "Расскажи подробно о 5 самых высоких горах Болгарии, их особенностях и маршрутах для хайкинга. Используй нумерованный список с переносами строк."
        }
    ];

    // Функция для форматирования текста - добавляет пробелы и переносы
    const formatText = (text) => {
        let formatted = text;

        // Добавляем пробелы после знаков препинания, если их нет
        formatted = formatted.replace(/([.,!?;:])([а-яА-Яa-zA-Z])/g, '$1 $2');

        // Добавляем пробелы между словами, где они должны быть
        formatted = formatted.replace(/([а-яА-Яa-zA-Z])([А-ЯA-Z])/g, '$1 $2'); // Между словами, где второе с заглавной
        formatted = formatted.replace(/(\d)([а-яА-Яa-zA-Z])/g, '$1 $2'); // Между цифрой и буквой
        formatted = formatted.replace(/([а-яА-Яa-zA-Z])(\d)/g, '$1 $2'); // Между буквой и цифрой

        // Обрабатываем специальные случаи для списков
        formatted = formatted.replace(/(\d+)\.([а-яА-Яa-zA-Z])/g, '$1. $2'); // "1.Мусала" -> "1. Мусала"
        formatted = formatted.replace(/(\d+),/g, '$1.'); // Заменяем запятые на точки в нумерации

        // Добавляем переносы строк для списков
        formatted = formatted.replace(/(\d+\.\s)/g, '\n$1'); // Перенос перед каждым пунктом списка

        return formatted;
    };

    // 📌 ОСНОВНАЯ ФУНКЦИЯ - с форматированием текста
    const startSSEStream = async (prompt) => {
        setIsLoading(true);
        setResponse('');
        setChunkCount(0);
        setTotalChars(0);
        setError('');

        abortControllerRef.current = new AbortController();

        try {
            const response = await fetch(streamUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    model: "gpt-4",
                    messages: [{
                        role: "user",
                        content: prompt + " Обязательно используй пробелы между словами и переносы строк между пунктами списка."
                    }],
                    temperature: 0.7
                }),
                signal: abortControllerRef.current.signal
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder();

            let buffer = '';
            let accumulatedText = '';

            const processChunk = async () => {
                try {
                    const { done, value } = await reader.read();

                    if (done) {
                        // Форматируем и выводим остаток текста
                        if (accumulatedText) {
                            const formattedText = formatText(accumulatedText);
                            setResponse(prev => prev + formattedText);
                            setTotalChars(prev => prev + formattedText.length);
                        }
                        setIsLoading(false);
                        return;
                    }

                    const chunk = decoder.decode(value, { stream: true });
                    buffer += chunk;

                    const lines = buffer.split('\n');
                    buffer = lines.pop() || '';

                    for (const line of lines) {
                        if (line.startsWith('data:')) {
                            const dataContent = line.substring(5).trim();

                            if (dataContent && dataContent !== '[DONE]') {
                                accumulatedText += dataContent;
                                setChunkCount(prev => prev + 1);

                                console.log('🔤 Character received:', dataContent);

                                // Форматируем и выводим накопленный текст при определенных условиях
                                const shouldFlush =
                                    dataContent.match(/[\s,.!?;:\n]/) || // Знаки препинания или пробелы
                                    accumulatedText.length > 15 || // Длина
                                    dataContent.match(/\d\./); // Начало нового пункта списка

                                if (shouldFlush && accumulatedText) {
                                    const formattedText = formatText(accumulatedText);
                                    setResponse(prev => prev + formattedText);
                                    setTotalChars(prev => prev + formattedText.length);
                                    accumulatedText = '';

                                    // Небольшая задержка для наглядности
                                    await new Promise(resolve => setTimeout(resolve, 30));
                                }
                            }
                        }
                    }

                    processChunk();
                } catch (error) {
                    if (error.name !== 'AbortError') {
                        console.error('Stream error:', error);
                        setError(`Ошибка: ${error.message}`);
                        setIsLoading(false);
                    }
                }
            };

            processChunk();

        } catch (error) {
            if (error.name !== 'AbortError') {
                console.error('Connection error:', error);
                setError(`Ошибка подключения: ${error.message}`);
                setIsLoading(false);
            }
        }
    };

    // Альтернативная версия - простое форматирование
    const startSSEStreamSimple = async (prompt) => {
        setIsLoading(true);
        setResponse('');
        setChunkCount(0);
        setTotalChars(0);
        setError('');

        abortControllerRef.current = new AbortController();

        try {
            const response = await fetch(streamUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    model: "gpt-4",
                    messages: [{
                        role: "user",
                        content: prompt + " Пожалуйста, используй правильные пробелы между словами и переносы строк между пунктами списка для лучшей читаемости."
                    }],
                    temperature: 0.7
                }),
                signal: abortControllerRef.current.signal
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder();

            let buffer = '';
            let fullText = '';

            const processChunk = async () => {
                try {
                    const { done, value } = await reader.read();

                    if (done) {
                        // Финальное форматирование всего текста
                        const formattedText = formatText(fullText);
                        setResponse(formattedText);
                        setIsLoading(false);
                        return;
                    }

                    const chunk = decoder.decode(value, { stream: true });
                    buffer += chunk;

                    const lines = buffer.split('\n');
                    buffer = lines.pop() || '';

                    for (const line of lines) {
                        if (line.startsWith('data:')) {
                            const dataContent = line.substring(5).trim();

                            if (dataContent && dataContent !== '[DONE]') {
                                fullText += dataContent;
                                setChunkCount(prev => prev + 1);
                                setTotalChars(prev => prev + dataContent.length);

                                // Форматируем и обновляем текст
                                const formattedText = formatText(fullText);
                                setResponse(formattedText);

                                console.log('🔤 Character received:', dataContent);

                                // Задержка для наглядности стриминга
                                await new Promise(resolve => setTimeout(resolve, 20));
                            }
                        }
                    }

                    processChunk();
                } catch (error) {
                    if (error.name !== 'AbortError') {
                        console.error('Stream error:', error);
                        setError(`Ошибка: ${error.message}`);
                        setIsLoading(false);
                    }
                }
            };

            processChunk();

        } catch (error) {
            if (error.name !== 'AbortError') {
                console.error('Connection error:', error);
                setError(`Ошибка подключения: ${error.message}`);
                setIsLoading(false);
            }
        }
    };

    const stopStream = () => {
        if (abortControllerRef.current) {
            abortControllerRef.current.abort();
            console.log('🛑 Stream stopped by user');
        }
        setIsLoading(false);
    };

    const clearAll = () => {
        stopStream();
        setResponse('');
        setChunkCount(0);
        setTotalChars(0);
        setError('');
    };

    return (
        <div style={{ maxWidth: 800, margin: '0 auto', padding: 20 }}>
            <h1>🧪 Тестирование SSE Stream API</h1>

            <div style={{ margin: '20px 0' }}>
                <h3>Быстрые тесты:</h3>
                <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', marginBottom: '15px' }}>
                    {testRequests.map((test, index) => (
                        <button
                            key={index}
                            onClick={() => startSSEStreamSimple(test.prompt)}
                            disabled={isLoading}
                            style={{
                                padding: '10px 15px',
                                border: '1px solid #007bff',
                                background: isLoading ? '#f8f9fa' : 'white',
                                color: isLoading ? '#6c757d' : '#007bff',
                                borderRadius: '5px',
                                cursor: isLoading ? 'not-allowed' : 'pointer',
                                opacity: isLoading ? 0.6 : 1
                            }}
                        >
                            {isLoading ? '🔄 Стриминг...' : test.name}
                        </button>
                    ))}
                </div>

                <div style={{ display: 'flex', gap: '10px', marginBottom: '15px' }}>
                    <button
                        onClick={stopStream}
                        disabled={!isLoading}
                        style={{
                            padding: '8px 16px',
                            border: '1px solid #dc3545',
                            background: !isLoading ? '#f8f9fa' : 'white',
                            color: '#dc3545',
                            borderRadius: '5px',
                            cursor: !isLoading ? 'not-allowed' : 'pointer',
                            opacity: !isLoading ? 0.6 : 1
                        }}
                    >
                        ⏹️ Остановить
                    </button>

                    <button
                        onClick={clearAll}
                        style={{
                            padding: '8px 16px',
                            border: '1px solid #6c757d',
                            background: 'white',
                            color: '#6c757d',
                            borderRadius: '5px',
                            cursor: 'pointer'
                        }}
                    >
                        🧹 Очистить
                    </button>
                </div>
            </div>

            <div style={{
                display: 'flex',
                gap: '20px',
                margin: '15px 0',
                padding: '15px',
                background: '#f8f9fa',
                borderRadius: '6px',
                flexWrap: 'wrap'
            }}>
                <div>📊 Чанков: <strong>{chunkCount}</strong></div>
                <div>🔢 Символов: <strong>{totalChars}</strong></div>
                <div>🔄 Статус: <strong>{isLoading ? 'Идет стриминг...' : 'Готов'}</strong></div>
            </div>

            {error && (
                <div style={{
                    background: '#f8d7da',
                    color: '#721c24',
                    padding: '10px',
                    borderRadius: '4px',
                    margin: '10px 0'
                }}>
                    ❌ {error}
                </div>
            )}

            <div>
                <h3>Ответ в реальном времени:</h3>
                <div
                    style={{
                        border: '2px solid #e9ecef',
                        borderRadius: '8px',
                        padding: '20px',
                        minHeight: '300px',
                        maxHeight: '500px',
                        overflowY: 'auto',
                        background: '#fafafa',
                        whiteSpace: 'pre-wrap',
                        lineHeight: '1.6',
                        fontFamily: 'Arial, sans-serif',
                        fontSize: '16px'
                    }}
                >
                    {response || 'Нажмите на кнопку выше чтобы начать тест...'}
                    <div ref={responseEndRef} />
                </div>
            </div>
        </div>
    );
};

export default EnhancedStreamTest;