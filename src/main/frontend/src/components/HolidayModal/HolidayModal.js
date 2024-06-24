import React, {useState, useEffect, useRef} from 'react';
import Dialog from "../Dialog/Dialog";
import './HolidayModal.css';
import {bool, func} from "prop-types";
import {holidayPropType, yearCalendarStatePropType} from "../Context/Shared";

const initData = {
    id: 0,
    title: "",
    date: ""
};

function HolidayModal({onSubmit, isOpen, onClose, onRemove, yearCalendarState}) {
    const focusInputRef = useRef(null);
    const [formState, setFormState] = useState(initData);

    const errors = {
        title: ""
    }

    useEffect(() => {
        if (isOpen && focusInputRef.current) {
            setTimeout(() => {
                focusInputRef.current.focus();
            }, 0);

            setFormState({
                ...formState,
                ...yearCalendarState.currentEvent
            });
        }
    }, [isOpen]);

    const handleSubmit = (event) => {
        event.preventDefault();
        if (formState.title.trim() === '') {
            errors.title = "Title can not be empty"
            console.error(errors.title);
        } else {
            errors.title = ""
            onSubmit(formState);
            setFormState(initData);
        }
    };

    const handleInputChange = (event) => {
        const {name, value} = event.target;
        setFormState((prevFormData) =>
            ({
                    ...prevFormData,
                    [name]: value
                }
            ));
    };

    const handleDelete = (event) => {
        event.preventDefault();

        onRemove(formState);
        setFormState(initData);
    };

    const isSaveBtn = true;

    // do not display Delete btn in case holiday.id = 0 (then, we are adding a new event)
    const isDeleteBtn = formState.id > 0;


    return (
        <Dialog isOpen={isOpen} onClose={onClose}
                isSaveBtn={isSaveBtn} handleSaveButton={handleSubmit}
                isDeleteBtn={isDeleteBtn} handleDeleteButton={handleDelete}>
            <form action="">
                <div className="Form_row">
                    <label htmlFor="title">Holiday title</label>
                    <input
                        ref={focusInputRef}
                        type="text"
                        id="title"
                        name="title"
                        value={formState.title}
                        onChange={handleInputChange}
                        required
                    />
                    <input
                        type="hidden"
                        id="date"
                        name="date"
                        value={formState.date}
                    />
                    <input
                        type="hidden"
                        id="id"
                        name="id"
                        value={formState.id}
                    />
                </div>
            </form>
        </Dialog>
    );
}

HolidayModal.propTypes = {
    yearCalendarState: yearCalendarStatePropType.isRequired,
    isOpen: bool.isRequired,
    onSubmit: func.isRequired,
    onClose: func,
    onRemove: func
};

HolidayModal.propTypes = {
    onSubmit: func.isRequired,
    inputObject: holidayPropType.isRequired
}

export default HolidayModal;