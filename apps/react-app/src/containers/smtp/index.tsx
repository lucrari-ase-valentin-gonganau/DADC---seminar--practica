"use client";

import axios from "axios";
import { useEffect, useState } from "react";

interface SNMPData {
  _id: string;
  name: string;
  oid: string;
  value: string;
  timestamp: string;
}

const path = process.env.BASE_PATH_URL_SNMP || "http://localhost:8082";

const SMTP = () => {
  const [data, setData] = useState<SNMPData[]>([]);
  const [loading, setLoading] = useState(true);
  const [limit, setLimit] = useState(10);
  const [offset, setOffset] = useState(0);
  const [search, setSearch] = useState("");

  useEffect(() => {
    fetchData(search);

    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [limit, offset, search]);

  const fetchData = async (searchTerm: string = "") => {
    try {
      const response = await axios.get(`${path}/snmp`, {
        params: { limit, offset, search: searchTerm },
      });

      setData(Array.isArray(response.data.data) ? response.data.data : []);
    } catch (error) {
      console.error("Error fetching SNMP data:", error);
      setData([]);
    } finally {
      setLoading(false);
    }
  };

  const onSearch = () => {
    setOffset(0);
    fetchData(search);
  };

  const handlePrevious = () => {
    setOffset((prev) => Math.max(0, prev - limit));
  };

  const handleNext = () => {
    setOffset((prev) => prev + limit);
  };

  return (
    <div className="container mt-4">
      <h2 className="mb-4">SNMP Data</h2>

      <div className="row mb-3">
        <div className="col-md-4">
          <label htmlFor="searchInput" className="form-label">
            Search:
          </label>
          <div className="input-group">
            <input
              id="searchInput"
              type="text"
              className="form-control"
              placeholder="Search by name, OID, or value..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
            />
            <button
              className="btn btn-primary"
              type="button"
              onClick={onSearch}
            >
              <i className="bi bi-search"></i> Search
            </button>
          </div>
        </div>
        <div className="col-md-3">
          <label htmlFor="limitSelect" className="form-label">
            Items per page:
          </label>
          <select
            id="limitSelect"
            className="form-select"
            value={limit}
            onChange={(e) => {
              setLimit(Number(e.target.value));
              setOffset(0);
            }}
          >
            <option value={5}>5</option>
            <option value={10}>10</option>
            <option value={20}>20</option>
            <option value={50}>50</option>
          </select>
        </div>
        <div className="col-md-5 d-flex align-items-end">
          <button
            className="btn btn-primary me-2"
            onClick={handlePrevious}
            disabled={offset === 0}
          >
            Previous
          </button>
          <button
            className="btn btn-primary me-2"
            onClick={handleNext}
            disabled={data.length < limit}
          >
            Next
          </button>
          <span className="text-muted">
            Showing {offset + 1} - {offset + data.length}
          </span>
        </div>
      </div>

      {loading ? (
        <div className="text-center">
          <div className="spinner-border" role="status">
            <span className="visually-hidden">Loading...</span>
          </div>
        </div>
      ) : (
        <table className="table table-striped table-bordered">
          <thead className="table-dark">
            <tr>
              <th>Name</th>
              <th>OID</th>
              <th>Value</th>
              <th>Timestamp</th>
            </tr>
          </thead>
          <tbody>
            {data?.map((item) => (
              <tr key={item._id}>
                <td>{item.name}</td>
                <td>{item.oid}</td>
                <td>{item.value}</td>
                <td>{new Date(item.timestamp).toLocaleString()}</td>
              </tr>
            ))}
          </tbody>
        </table>
      )}
    </div>
  );
};

export default SMTP;
