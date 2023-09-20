import {useEffect} from 'react';
import {useConfig} from '../Context/Config.js';
import useSWR from "swr";
import {func} from "prop-types";
import {yearCalendarStatePropType} from "../Context/Shared";

/**
 * GetHolidays fetcher by year hook
 */
const useFetchByYear = (year) => {
    const {apiUrl} = useConfig();
    const getUrl = `${apiUrl}?year=${year}`;
    const eventFetcher = (url) => fetch(url).then((res) => res.json());
    const {data, error, isLoading} = useSWR(getUrl, eventFetcher);

    return {
        holidayEventsData: data,
        getHolidayEventsError: error,
        isLoading
    };
};

const fetchItems = async (url) => {
    // fetch holiday items
    const response = await fetch(url);
    return await response.json();
};


/**
 * GetHolidays fetcher component to get events by year
 */
function GetHolidays({yearCalendarState, setYearCalendarState}) {

    // [!] Solution with SWR
    const {holidayEventsData, getHolidayEventsError} = useFetchByYear(yearCalendarState.selectedYear);

    useEffect(() => {
        if (getHolidayEventsError) {
            console.error(getHolidayEventsError);
        } else {
            setYearCalendarState({...yearCalendarState, holidayEvents: holidayEventsData});
        }
    }, [holidayEventsData]);


    // [!] Solution with pure fetch()
    // const {apiUrl} = useConfig();
    //
    // useEffect(() => {
    //     fetchItems(`${apiUrl}?year=${yearCalendarState.selectedYear}`)
    //         .then((data) => {
    //             if (data.error) {
    //                 console.error(data.error);
    //             }
    //             else {
    //                 setYearCalendarState({...yearCalendarState, holidayEvents: data});
    //             }
    //         });
    // }, [yearCalendarState.selectedYear]);

}

GetHolidays.propTypes = {
    yearCalendarState: yearCalendarStatePropType.isRequired,
    setYearCalendarState: func.isRequired
};

export default GetHolidays;