import { createContext, useContext } from 'react';

const ConfigContext = createContext();


export  const ConfigProvider = ({ children }) => {
    const config = {
      apiUrl: "http://localhost:8080/holidays",
      apiKey: "the_api_key", 
    };
  
    return <ConfigContext.Provider value={config}>{children}</ConfigContext.Provider>;
  };

export const useConfig = () => {
    return useContext(ConfigContext);
  };