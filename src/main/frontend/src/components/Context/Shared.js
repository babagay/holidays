import PropTypes, {arrayOf, number, shape, string} from "prop-types";

export const holidayPropType = shape({
    id: number,
    title: string,
    date: string
});

export const yearCalendarStatePropType = shape({
    holidayEvents: arrayOf(PropTypes.shape({
        id: number,
        title: string,
        date: string
    })),
    selectedYear: number,
    currentEvent: holidayPropType
});

