import React from 'react';
import FullCalendar from '@fullcalendar/react';
import interactionPlugin from "@fullcalendar/interaction";
import multiMonthPlugin from "@fullcalendar/multimonth";
import './YearCalendar.css';
import {initYearCalendarState} from "../../App";
import 'react-tooltip/dist/react-tooltip.css'
import {func} from "prop-types";
import {yearCalendarStatePropType} from "../Context/Shared";

function YearCalendar({yearCalendarState, setYearCalendarState, setHolidayModalOpen}) {

    /**
     * Update holiday
     * Process click on the event
     */
    const handleEventClick = (info) => {

        const id = parseInt(info.event._def.publicId);
        const date = info.event.start.toISOString();
        const title = info.event.title;

        setYearCalendarState({
            ...yearCalendarState,
            currentEvent: {
                title,
                date,
                id
            }
        });
        setHolidayModalOpen(true);
    }

    /**
     * Add holiday
     * Process click on cell
     */
    const handleDateClick = (arg) => {

        const existentHoliday = yearCalendarState.holidayEvents.find(item =>
            item.date.startsWith(arg.dateStr)
        );

        if (existentHoliday) {
            // switch to edit mode if user clicked on free space of the cell but there is existent holiday on this date
            console.log("Another event found on this date", existentHoliday);
            setYearCalendarState({...yearCalendarState, currentEvent: {...existentHoliday}});
        } else {
            const currentEvent = {...initYearCalendarState.currentEvent, date: arg.dateStr};
            setYearCalendarState({...yearCalendarState, currentEvent});
        }

        setHolidayModalOpen(true);
    };
    const handleSetDates = (dateInfo) => {
        // update Year
        const newYear = dateInfo.start.getFullYear();
        if (newYear !== yearCalendarState.selectedYear) {
            setYearCalendarState({...yearCalendarState, selectedYear: newYear});
        }
    };

    return (
        <div>
            <FullCalendar
                plugins={[multiMonthPlugin, interactionPlugin]} // Specify the plugins to use
                initialView="multiMonthYear" // Set the initial view (e.g., month, week, day)
                dateClick={handleDateClick}
                datesSet={handleSetDates}
                events={yearCalendarState.holidayEvents}
                eventClick={handleEventClick}
                headerToolbar={{
                    left: 'prevYear',
                    center: 'title',
                    right: 'nextYear',
                }}
            >
            </FullCalendar>
        </div>
    );
}

YearCalendar.propTypes = {
    yearCalendarState: yearCalendarStatePropType.isRequired,
    setYearCalendarState: func.isRequired,
    setHolidayModalOpen: func.isRequired
};

export default YearCalendar;