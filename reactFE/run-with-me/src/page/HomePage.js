import MainHeadLineComponent from "../components/Layout/MainHeadLineComponent";
import WellComeComponent from "../components/Layout/WellComeComponent";
import ReportMain from "../components/Report/ReportMain";
import SideBar from "../components/Layout/SideBar";

import { useEffect, useState } from "react";

import styles from "./HomePage.module.css";
import { useSelector } from "react-redux";
import { Route, Routes, useNavigate } from "react-router-dom";
const HomeMain = () => {
  const [sideBarVisible, setsideBarVisible] = useState(true);
  const [toggle, setToggle] = useState(false);

  const auth = useSelector((state) => state.auth);

  const navigate = useNavigate();

  useEffect(() => {
    if (auth.accessToken == null || auth.accessToken == "") {
      navigate("/login");
    }
  }, [auth]); //auth에 변화가 있을 떄

  return (
    <div className={styles.home}>
      <div className={styles.side}>{!toggle && <SideBar></SideBar>}</div>
      <div className={styles.main}>
        <div className={styles["main-upside"]}>
          <MainHeadLineComponent />
          <WellComeComponent />
        </div>
        <div>
          <Routes>
            <Route index element={<h1>홈</h1>} />
            <Route path="report" element={<ReportMain />} />
            <Route path="other" element={<h1>다른 페이지</h1>} />
          </Routes>
        </div>
        <div></div>
      </div>
    </div>
  );
};

export default HomeMain;
