import {array, number, shape, string} from "prop-types";

export const holidayPropType = shape({
    id: number,
    title: string,
    date: string
});

export const yearCalendarStatePropType = shape({
    holidayEvents: array,
    selectedYear: number,
    currentEvent: holidayPropType
});

