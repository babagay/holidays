import logo from './logo.svg';
import './App.css';
import GetHolidays from './components/Holidays/GetHolidays';
import YearCalendar from "./components/YearCalendar/YearCalendar";
import {useState} from "react";
import HolidayModal from "./components/HolidayModal/HolidayModal";
import ModifyHolidays from "./components/Holidays/ModifyHolidays";

const initHolidayDTO = {add: false, update: false, remove: false, holiday: {}};
export const initYearCalendarState = {
    holidayEvents: [],
    selectedYear: new Date().getFullYear(),
    currentEvent: {
        title: "",
        date: "",
        id: 0
    }
}

function App() {

    const [isHolidayModalOpen, setHolidayModalOpen] = useState(false);
    const [yearCalendarState, setYearCalendarState] = useState(initYearCalendarState);
    const [holidayDTO, setHolidayDTO] = useState(initHolidayDTO);

    const handleHolidayFormSubmit = (formData) => {
        // start operation of add/insert a holiday

        const insert = formData.id === 0;
        const update = formData.id > 0;
        const holiday = {...formData, date: new Date(formData.date).toISOString()};

        setHolidayDTO({...holidayDTO, add: insert, update: update, holiday});
        handleCloseHolidayModal();
    };

    const handleHolidayItemRemove = (formData) => {
        // start operation of removing a Holiday

        const holiday = {...formData, date: new Date(formData.date).toISOString()};
        setHolidayDTO({...holidayDTO, remove: true, holiday});
        handleCloseHolidayModal();
    };

    const handleOpenHolidayModal = () => {
        setHolidayModalOpen(true);
    };

    const handleCloseHolidayModal = () => {
        setHolidayModalOpen(false);
    };

    const handleHolidayCreate = (data) => {
        const id = data.holidays[0]?.id;

        if (id) {
            const holiday = {...holidayDTO.holiday, id};
            const events = [...yearCalendarState.holidayEvents, holiday];

            setYearCalendarState({...yearCalendarState, holidayEvents: events});
        }
    };
    const handleHolidayUpdate = () => {
        // update item in collection after it was updated on back-end
        const eventsWithoutCurrent = yearCalendarState.holidayEvents.filter(u => u.id !== holidayDTO.holiday.id);

        setYearCalendarState({...yearCalendarState, holidayEvents: [...eventsWithoutCurrent, holidayDTO.holiday]});
    };
    const handleHolidayDelete = () => {
        // remove event from collection
        const updatedEvents = yearCalendarState.holidayEvents.filter(item => item.id !== holidayDTO.holiday.id);
        setYearCalendarState({...yearCalendarState, holidayEvents: [...updatedEvents]});
    };
    const handleHolidayModificationFinish = () => {
        setHolidayDTO(initHolidayDTO);
    };

    const holidayModificationCallbacks = {
        onCreate: handleHolidayCreate,
        onUpdate: handleHolidayUpdate,
        onDelete: handleHolidayDelete,
        onFinish: handleHolidayModificationFinish
    };

    return (
        <div className="App">
            <header className="App-header">
                <img src={logo} className="App-logo" alt="logo"/>
            </header>
            <div className="App-body">
                <>
                    <GetHolidays
                        yearCalendarState={yearCalendarState}
                        setYearCalendarState={setYearCalendarState}/>
                    <ModifyHolidays holidayDTO={holidayDTO}
                                    callbacks={holidayModificationCallbacks}/>
                </>
                <YearCalendar
                    setHolidayModalOpen={setHolidayModalOpen}
                    yearCalendarState={yearCalendarState}
                    setYearCalendarState={setYearCalendarState}/>
                <HolidayModal
                    isOpen={isHolidayModalOpen}
                    yearCalendarState={yearCalendarState}
                    onSubmit={handleHolidayFormSubmit}
                    onRemove={handleHolidayItemRemove}
                    onClose={handleCloseHolidayModal}/>
            </div>
        </div>
    );
}

export default App;
