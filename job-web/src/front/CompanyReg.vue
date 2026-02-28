<template>
    <div style="width: 500px;margin: auto">
        <r-form ref="myForm" :rules="rules" :form="form" :items="items" :save="save">
            <template v-slot:extra>
                <el-form-item label="联系电话" prop="telephone">
                    <div style="display:flex;gap:8px;">
                        <el-input v-model="form.telephone" placeholder="联系电话"
                                  :disabled="!turnstileToken" style="flex:1;" />
                        <el-button :disabled="!turnstileToken || smsCountdown > 0"
                                   @click="sendSmsCode">
                            {{ smsCountdown > 0 ? smsCountdown + 's 后重试' : '发送验证码' }}
                        </el-button>
                    </div>
                </el-form-item>
                <el-form-item label="验证码" prop="smsCode">
                    <el-input v-model="form.smsCode" placeholder="请输入验证码"
                              :disabled="!turnstileToken" />
                </el-form-item>
                <el-form-item>
                    <div class="turnstile" ref="turnstile"></div>
                    <div v-if="turnstileError" class="turnstile-error">{{ turnstileError }}</div>
                </el-form-item>
            </template>
        </r-form>
    </div>
</template>

<script>
    import RForm from "../components/RForm";
    import {company_create, sms_send, sms_check} from "@/api/front";
    import {message} from "@/utils/message";

    export default {
        name: "CompanyReg",
        components: {RForm},
        data() {
            let validatePwd = (_rule, value, callback) => {
                if (value === '') {
                    callback(new Error('请再次输入密码'));
                } else if (value !== this.form.password) {
                    callback(new Error('两次输入密码不一致!'));
                } else {
                    callback();
                }
            }
            return {
                form: {
                    name: '',
                    account: null,
                    password: '',
                    password2: '',
                    contact: '',
                    telephone: '',
                    smsCode: '',
                },
                turnstileWidgetId: null,
                turnstileToken: null,
                turnstileError: '',
                smsCountdown: 0,
                smsTimer: null,
                rules: {
                    name: [{required: true, message: '必填项不能为空'}],
                    account: [{required: true, message: '必填项不能为空'}],
                    password: [{required: true, message: '必填项不能为空'}],
                    password2: [
                        {required: true, message: '必填项不能为空'},
                        {required: true, validator: validatePwd}
                    ],
                    telephone: [{required: true, message: '必填项不能为空'}],
                    smsCode: [{required: true, message: '验证码不能为空'}],
                },
                items: [
                    {type: 'text', label: '姓名', prop: 'name', name: 'name', placeholder: '请输入姓名'},
                    {type: 'text', label: '账号', prop: 'account', name: 'account', placeholder: '账号'},
                    {type: 'password', label: '密码', prop: 'password', name: 'password', placeholder: '密码'},
                    {type: 'password', label: '确认密码', prop: 'password2', name: 'password2', placeholder: '确认密码'},
                    {type: 'text', label: '联系人', prop: 'contact', name: 'contact', placeholder: '联系人'},
                ]
            }
        },
        mounted() {
            this.initTurnstile();
        },
        beforeDestroy() {
            this.removeTurnstile();
            if (this.smsTimer) clearInterval(this.smsTimer);
        },
        methods: {
            getTurnstileSiteKey() {
                return process.env.VUE_APP_TURNSTILE_SITEKEY;
            },
            waitForTurnstileApi({timeoutMs = 8000, intervalMs = 50} = {}) {
                return new Promise((resolve, reject) => {
                    const startedAt = Date.now();
                    const timer = setInterval(() => {
                        if (window.turnstile && typeof window.turnstile.render === 'function') {
                            clearInterval(timer);
                            resolve(window.turnstile);
                            return;
                        }
                        if (Date.now() - startedAt >= timeoutMs) {
                            clearInterval(timer);
                            reject(new Error('Turnstile 脚本加载超时'));
                        }
                    }, intervalMs);
                });
            },
            async initTurnstile() {
                const sitekey = this.getTurnstileSiteKey();
                if (!sitekey) {
                    this.turnstileError = '未配置 Turnstile sitekey（VUE_APP_TURNSTILE_SITEKEY）';
                    message.warning(this.turnstileError);
                    return;
                }
                if (!this.$refs.turnstile) {
                    return;
                }

                try {
                    this.turnstileError = '';
                    const turnstile = await this.waitForTurnstileApi();
                    this.turnstileToken = null;
                    this.turnstileWidgetId = turnstile.render(this.$refs.turnstile, {
                        sitekey,
                        theme: 'auto',
                        callback: (token) => {
                            this.turnstileToken = token;
                        },
                        'expired-callback': () => {
                            this.turnstileToken = null;
                        },
                        'error-callback': () => {
                            this.turnstileToken = null;
                        }
                    });
                } catch (e) {
                    const detail = e && e.message ? e.message : 'Turnstile 初始化失败';
                    this.turnstileError = detail + '（请检查浏览器能否访问 https://challenges.cloudflare.com ）';
                    // eslint-disable-next-line no-console
                    console.error('[Turnstile] init failed:', e);
                    message.error(this.turnstileError);
                }
            },
            resetTurnstile() {
                if (window.turnstile && this.turnstileWidgetId !== null) {
                    try {
                        window.turnstile.reset(this.turnstileWidgetId);
                    } catch (e) {
                        // ignore
                    }
                }
                this.turnstileToken = null;
            },
            removeTurnstile() {
                if (window.turnstile && this.turnstileWidgetId !== null) {
                    try {
                        window.turnstile.remove(this.turnstileWidgetId);
                    } catch (e) {
                        // ignore
                    }
                }
                this.turnstileWidgetId = null;
                this.turnstileToken = null;
            },
            sendSmsCode() {
                sms_send({phoneNumber: this.form.telephone, templateCode: '100001'})
                    .then(() => {
                        message.success('验证码已发送');
                        this.startSmsCountdown();
                    }).catch(() => {});
            },
            startSmsCountdown() {
                this.smsCountdown = 60;
                this.smsTimer = setInterval(() => {
                    if (--this.smsCountdown <= 0) {
                        clearInterval(this.smsTimer);
                        this.smsTimer = null;
                    }
                }, 1000);
            },
            save() {
                let flag = this.$refs['myForm'].validateForm();
                if (flag) {
                    if (!this.turnstileToken) {
                        message.warning('请先完成人机验证');
                        return;
                    }
                    sms_check({phoneNumber: this.form.telephone, code: this.form.smsCode})
                        .then(() => {
                            let param = {
                                ...this.form,
                                turnstileToken: this.turnstileToken
                            };
                            delete param.smsCode;
                            company_create(param).then(res => {
                                this.$message.success(res.msg);
                                setTimeout(() => { this.$router.push('/'); }, 1500);
                            }).catch(() => {
                                this.resetTurnstile();
                            });
                        }).catch(() => {});
                }
            }
        }

    }
</script>

<style scoped>
    .turnstile {
        min-height: 66px;
    }

    .turnstile-error {
        margin-top: 8px;
        color: #F56C6C;
        font-size: 12px;
        line-height: 1.4;
        word-break: break-all;
    }
</style>
