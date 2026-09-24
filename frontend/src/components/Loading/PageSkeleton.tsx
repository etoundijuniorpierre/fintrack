import { Skeleton } from "antd";
import styles from "./PageLoader.module.scss";

export const PageSkeleton = () => {
  return (
    <div
      className={styles.spinner}
      style={{
        padding: "24px",
        width: "100%",
        height: "100%",
        display: "flex",
        flexDirection: "column",
        gap: "24px",
      }}
    >
      <Skeleton.Button active block style={{ height: "64px" }} />
      <Skeleton active paragraph={{ rows: 8 }} />
    </div>
  );
};
