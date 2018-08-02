import { Moment } from 'moment';
import { IUser } from 'app/core/user/user.model';

export interface IUserExtra {
    id?: number;
    birthDate?: Moment;
    type?: number;
    user?: IUser;
}

export class UserExtra implements IUserExtra {
    constructor(public id?: number, public birthDate?: Moment, public type?: number, public user?: IUser) {}
}
