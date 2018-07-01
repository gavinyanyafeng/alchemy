import { Component } from '@angular/core';
import { ConfService } from './conf.service';
import { ActivatedRoute, Router } from '@angular/router';
import { Conf } from '../model/conf.model';
import 'codemirror/mode/yaml/yaml';
import 'codemirror/mode/groovy/groovy';
import 'codemirror/mode/sql/sql';
import { JhiAlertService } from 'ng-jhipster';

@Component({
    selector: 'jhi-conf',
    templateUrl: 'conf.component.html'
})
export class ConfComponent {
    conf: Conf;

    yamlConfig: any = { lineNumbers: true, mode: 'text/x-yaml', theme: 'material' };

    groovyConfig: any = { lineNumbers: true, mode: 'text/x-groovy', theme: 'material' };

    sqlConfig: any = { lineNumbers: true, mode: 'text/x-sql' };

    constructor(
        private confService: ConfService,
        private alertService: JhiAlertService,
        private route: ActivatedRoute,
        private router: Router
    ) {
        this.route.data.subscribe(({ conf }) => {
            this.conf = conf.body ? conf.body : new Conf();
            if (!this.conf.content.code || this.conf.content.code.length == 0) {
                this.conf.content.code = [''];
            }
            if (!this.conf.content.config) {
                this.conf.content.config = '';
            }
        });
    }

    ngOnInit() {}

    add() {
        if (!this.conf.content.code) {
            this.conf.content.code = [];
        }
        this.conf.content.code.push('');
    }

    delete(index) {
        this.conf.content.code.slice(index, 1);
    }

    save() {
        if (this.conf.id !== null) {
            this.confService.update(this.conf).subscribe(response => this.onSaveSuccess(response), () => this.onSaveError());
        } else {
            this.confService.create(this.conf).subscribe(response => this.onSaveSuccess(response), () => this.onSaveError());
        }
    }

    previousState() {
        this.router.navigate(['']);
    }

    private onSaveSuccess(result) {
        this.router.navigate(['']);
    }

    private onSaveError(error) {
        this.alertService.error(error.error, error.message, null);
    }
}
