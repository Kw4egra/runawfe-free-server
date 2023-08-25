import Constants from "./Constants";

export class Options {
    page: number;
    sortBy: string[];
    sortDesc: boolean[];
    groupBy: string[];
    groupDesc: boolean[];
    multiSort: boolean;
    mustSort: boolean;

    constructor() {
        this.page = 1;
        this.sortBy = [];
        this.sortDesc = [];
        this.groupBy = [];
        this.groupDesc = [];
        this.multiSort = false;
        this.mustSort = true;
    }
}

export class Sorting {
    name: string;
    order: string;
    constructor(name: string, order: string = 'desc') {
        this.name = name;
        this.order = order;
    }
    static convert(sortBy: string[], sortDesc: boolean[]) {
        let listSortings: Sorting[] = [];
        for (let i=0; i < sortBy.length; i++) {
            listSortings.push(new Sorting(sortBy[i], sortDesc[i] ? 'desc' : 'asc'));
        }
        return listSortings;
    }
}

export class Select {
    text: string;
    value: string;

    constructor(text: string, value: string){
        this.text = text ?? '';
        this.value = value ?? '';
    }
}

export class Header {
    text: string;
    align: string;
    width: string;
    bcolor: string;
    sortable: boolean;
    visible: boolean;
    dynamic: boolean;
    format: string;
    link: boolean;
    selectOptions: Select[];
    value: string;
    filterable: boolean;

    constructor() {
        this.text = '';
        this.align = '';
        this.value = '';
        this.visible = true;
        this.width = '';
        this.sortable = false;
        this.dynamic = false;
        this.format = 'String';
        this.link = false;
        this.bcolor = Constants.WHITE_COLOR;
        this.selectOptions = [];
        this.filterable = true;
    }
}
