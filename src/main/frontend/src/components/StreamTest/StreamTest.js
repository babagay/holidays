import React, { useState } from 'react';
import './StreamTest.css';

const StreamTest = () => {
    const [response, setResponse] = useState('');
    const [isLoading, setIsLoading] = useState(false);
    const [chunkCount, setChunkCount] = useState(0);
    const [totalChars, setTotalChars] = useState(0);

     const {apiUrl} = useConfig();
     const streamUrl = `${apiUrl}/chat/stream/test-flux`; // .../stream/flux

    const testSlowStream = async () => {
        setIsLoading(true);
        setResponse('');
        setChunkCount(0);
        setTotalChars(0);

        try {
            const response = await fetch(streamUrl, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json',
                },
                body: JSON.stringify({
                    model: "gpt-4",
                    messages: [
                        {
                            role: "user",
                            content: "Расскажи подробно о 10 самых высоких горах Болгарии, их особенностях, маршрутах для хайкинга и лучшем времени для посещения. Ответ должен быть развернутым и содержательным."
                        }
                    ],
                    temperature: 0.7,
                    maxTokens: 1000
                })
            });

            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder();

            while (true) {
                const { done, value } = await reader.read();

                if (done) {
                    setIsLoading(false);
                    break;
                }

                const chunk = decoder.decode(value, { stream: true });

                setResponse(prev => prev + chunk);
                setChunkCount(prev => prev + 1);
                setTotalChars(prev => prev + chunk.length);

                // Автопрокрутка
                setTimeout(() => {
                    const responseElement = document.getElementById('response-area');
                    if (responseElement) {
                        responseElement.scrollTop = responseElement.scrollHeight;
                    }
                }, 0);
            }

        } catch (error) {
            console.error('Error:', error);
            setResponse(prev => prev + `\n\n❌ Ошибка: ${error.message}`);
            setIsLoading(false);
        }
    };

    return (
        <div className="stream-test-container">
            <h1>🧪 Тестирование Slow Stream</h1>

            <div className="controls">
                <button
                    onClick={testSlowStream}
                    disabled={isLoading}
                    className="test-button"
                >
                    {isLoading ? '🔄 Стриминг...' : '🚀 Запустить тест'}
                </button>

                {isLoading && (
                    <div className="loading-indicator">
                        <div className="spinner"></div>
                        <span>Получаем ответ по частям...</span>
                    </div>
                )}
            </div>

            <div className="stats">
                <div>📊 Чанков получено: <strong>{chunkCount}</strong></div>
                <div>🔢 Всего символов: <strong>{totalChars}</strong></div>
            </div>

            <div className="response-container">
                <h3>Ответ (появляется постепенно):</h3>
                <div
                    id="response-area"
                    className="response-area"
                >
                    {response || (
                        <div className="placeholder">
                            Ответ появится здесь по частям...
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default StreamTest;