import axios, { CreateAxiosDefaults } from "axios";

export const ApiJavalin = (config?: CreateAxiosDefaults) => {
  const path = process.env.BASE_PATH_URL_JAVALIN || "http://localhost:8081";

  const apiClient = axios.create({
    baseURL: path,
    headers: {
      "Content-Type": "application/json",
    },
    ...config,
  });

  return apiClient;
};

export const ApiNode = (config?: CreateAxiosDefaults) => {
  const path = process.env.BASE_PATH_URL_NODE || "http://localhost:8082";

  const apiClient = axios.create({
    baseURL: path,
    headers: {
      "Content-Type": "application/json",
    },
    ...config,
  });

  return apiClient;
};
