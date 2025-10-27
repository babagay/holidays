import React, { useEffect, useState } from "react";

export default function StreamViewer() {
  const [messages, setMessages] = useState([]);

  useEffect(() => {
    const eventSource = new EventSource("http://localhost:8080/chat/stream/test-flux"); // test-flux-2

    eventSource.onmessage = (event) => {
      setMessages((prev) => [...prev, event.data]);
    };

    eventSource.onerror = (err) => {
      console.error("Ошибка стрима:", err);
      eventSource.close();
    };

    return () => eventSource.close();
  }, []);

  return (
    <div className="p-4">
      <h2>📡 Реактивный поток:</h2>
      <ul>
        {messages.map((msg, i) => (
          <li key={i}>{msg}</li>
        ))}
      </ul>
    </div>
  );
}
