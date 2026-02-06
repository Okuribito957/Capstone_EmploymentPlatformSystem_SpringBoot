<template>
    <el-dialog title="详情" :fullscreen="true" :visible.sync="param.visible" @close="param.close"
               :close-on-click-modal="false">
        <r-form ref="myForm" :form="form" :items="items" :save="save"></r-form>
    </el-dialog>
</template>

<script>
    import {update} from "@/api/send";
    import RForm from "@/components/RFormReadOnly";

    export default {
        name: "Detail",
        components: {RForm},
        props: {
            param: {
                type: Object,
                default: () => {
                }
            }
        },
        data() {
            return {
                form: {
                    id: '',
                    name: '',
                    jobStatus: '',
                    evaluate: '',
                    skill: '',
                    status: 1, // This status is for resume, not send status
                    send_status: '', // New field for send status
                },
                items: [
                    {type: 'text', label: '名称', prop: 'name', name: 'name', placeholder: '名称'},
                    {
                        type: 'select', label: '求职状态', prop: 'jobStatus', name: 'jobStatus', placeholder: '求职状态',
                        options: [{value: '待业可以立即上岗', label: '待业可以立即上岗'}, {value: '在岗考虑新工作', label: '在岗考虑新工作'}]
                    },
                    {type: 'area', label: '自我评价', prop: 'evaluate', name: 'evaluate', placeholder: '自我评价'},
                    {type: 'area', label: '技能描述', prop: 'skill', name: 'skill', placeholder: '技能描述'},
                    {
                        type: 'select', label: '状态', prop: 'status', name: 'status', placeholder: '状态',
                        options: [{value: 1, label: '开放'}, {value: 0, label: '关闭'}]
                    },
                    {
                        type: 'select', label: '投递状态', prop: 'send_status', name: 'send_status', placeholder: '投递状态',
                        isEditable: true, // Make this field editable
                        options: [
                            {value: 0, label: '待查看'},
                            {value: 1, label: '已查看'},
                            {value: 2, label: '有意向'},
                            {value: 3, label: '不合适'}
                        ]
                    },
                ]
            }
        },
        mounted() {
            // Initialize with default structure for resume, ensuring all fields exist
            const defaultResumeStructure = { 
                id: '', name: '', jobStatus: '', evaluate: '', skill: '', status: 1 
            };
            const resumeData = JSON.parse(JSON.stringify(this.param.form.resume || defaultResumeStructure));
            const session_send = JSON.parse(sessionStorage.getItem("session_send"));

            let sendStatusValue;
            if (this.param.form && typeof this.param.form.status !== 'undefined' && this.param.form.status !== null && this.param.form.status !== '') {
                const parsedNum = parseInt(this.param.form.status, 10);
                if (!isNaN(parsedNum)) {
                    sendStatusValue = parsedNum; // Ensure it's a number
                } else {
                    sendStatusValue = ''; // Fallback for non-numeric status
                }
            } else {
                // If status is not present, null, or empty string in this.param.form
                sendStatusValue = ''; // Default to empty string (placeholder will show)
            }
            
            // Construct the new form object, ensuring all expected properties are present
            const newForm = {
                // ...resumeData, // Spread resume data (includes defaults if resume was null)
                send_status: sendStatusValue, // Add/override send_status
                id: session_send.id,
                name: resumeData.name,
                jobStatus: resumeData.jobStatus,
                evaluate: resumeData.evaluate,
                skill: resumeData.skill,
                status: resumeData.status,
            };
            
            this.form = newForm;
        },
        methods: {
            save() {
                let flag = this.$refs['myForm'].validateForm();
                if (flag) {
                    this.form.status = this.form.send_status; // Update status to match send_status
                    update(this.form).then(res => {
                        this.$message.success(res.msg);
                        this.param.close();
                        this.param.callback();
                    })
                }
            }
        }
    }
</script>

<style scoped>

</style>