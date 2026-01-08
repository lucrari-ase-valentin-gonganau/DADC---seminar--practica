import mysql from "mysql2/promise";
import mongoose from "mongoose";

export const dbMysql = await mysql.createConnection({
  host: process.env.DB_HOST || "localhost",
  user: process.env.DB_USER || "stud",
  password: process.env.DB_PASSWORD || "stud123",
  database: process.env.DB_NAME || "blobstore",
});

export const dbMongo = await mongoose.connect(
  process.env.MONGO_URI ||
    "mongodb://stud:stud123@localhost:27017/snmpdb?authSource=admin"
);
