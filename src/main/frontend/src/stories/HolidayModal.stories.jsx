import React from "react";
import HolidayModal from "../components/HolidayModal/HolidayModal";
import '../App.css';

export default { title: "HolidayModal", component: HolidayModal };

const onSubmit = () => {
    console.log("Submit")
};

const onClose = () => {
    console.log("Close")
};

const onRemove = () => {
    console.log("Delete")
};

const stateAddItem = {
    holidayEvents: [],
    selectedYear: new Date().getFullYear(),
    currentEvent: {
        title: "New holiday Title",
        date: "2023-06-26T00:00:00Z",
        id: 0
    }
};

const stateUpdateItem = {
    holidayEvents: [],
    selectedYear: new Date().getFullYear(),
    currentEvent: {
        title: "Existent item Title",
        date: "2023-06-26T00:00:00Z",
        id: 2
    }
};

const stateDeleteItem = {
    holidayEvents: [],
    selectedYear: new Date().getFullYear(),
    currentEvent: {
        title: "Title",
        date: "2023-06-26T00:00:00Z",
        id: 10
    }
};

export const Add = () => (
    <HolidayModal
        isOpen={true}
        onSubmit={onSubmit}
        onClose={onClose}
        yearCalendarState={stateAddItem}
    />
);

export const Update = () => (
    <HolidayModal
        isOpen={true}
        onSubmit={onSubmit}
        onClose={onClose}
        yearCalendarState={stateUpdateItem}
    />
);

export const Delete = () => (
    <HolidayModal
        isOpen={true}
        onSubmit={onSubmit}
        onClose={onClose}
        onRemove={onRemove}
        yearCalendarState={stateDeleteItem}
    />
);
