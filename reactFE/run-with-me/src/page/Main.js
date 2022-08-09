import React, { useState } from "react";
import GoogleOauth from "../components/Login/GoogleOauth";
import { useSelector } from "react-redux";
import HomeMain from "./HomePage";
import { Route, Routes } from "react-router-dom";

const Main = () => {
  const auth = useSelector((state) => state.auth);
  console.log(auth);

  return (
    <>
      <Routes>
        <Route path="/login" element={<GoogleOauth />} />

        <Route path="/" element={<HomeMain />} />
      </Routes>
    </>
  );
};

export default Main;