package io.yamm.backend;

public class Enricher {
    public static Transaction categorise(Transaction transaction) {
        if (transaction.mcc == null) {
            return transaction;
        }

        switch(transaction.mcc) {
            case "0742":
                transaction.category = TransactionCategory.PETS;
                break;
            case "0763":
                transaction.category = TransactionCategory.GENERAL;
                break;
            case "0780":
            case "1520":
            case "1711":
            case "1731":
            case "1740":
            case "1750":
            case "1761":
            case "1771":
            case "1799":
                transaction.category = TransactionCategory.BILLS_AND_HOME;
                break;
            case "2741":
            case "2791":
                transaction.category = TransactionCategory.GENERAL;
                break;
            case "2842":
                transaction.category = TransactionCategory.HEALTH_AND_BEAUTY;
                break;
            case "3000":
            case "3001":
            case "3002":
            case "3003":
            case "3004":
            case "3005":
            case "3006":
            case "3007":
            case "3008":
            case "3009":
            case "3010":
            case "3011":
            case "3012":
            case "3013":
            case "3014":
            case "3015":
            case "3016":
            case "3017":
            case "3018":
            case "3019":
            case "3020":
            case "3021":
            case "3022":
            case "3023":
            case "3024":
            case "3025":
            case "3026":
            case "3027":
            case "3028":
            case "3029":
            case "3030":
            case "3031":
            case "3032":
            case "3033":
            case "3034":
            case "3035":
            case "3036":
            case "3037":
            case "3038":
            case "3039":
            case "3040":
            case "3041":
            case "3042":
            case "3043":
            case "3044":
            case "3045":
            case "3046":
            case "3047":
            case "3048":
            case "3049":
            case "3050":
            case "3051":
            case "3052":
            case "3053":
            case "3054":
            case "3055":
            case "3056":
            case "3057":
            case "3058":
            case "3059":
            case "3060":
            case "3061":
            case "3062":
            case "3063":
            case "3064":
            case "3065":
            case "3066":
            case "3067":
            case "3068":
            case "3069":
            case "3070":
            case "3071":
            case "3072":
            case "3073":
            case "3074":
            case "3075":
            case "3076":
            case "3077":
            case "3078":
            case "3079":
            case "3080":
            case "3081":
            case "3082":
            case "3083":
            case "3084":
            case "3085":
            case "3086":
            case "3087":
            case "3088":
            case "3089":
            case "3090":
            case "3091":
            case "3092":
            case "3093":
            case "3094":
            case "3095":
            case "3096":
            case "3097":
            case "3098":
            case "3099":
            case "3100":
            case "3101":
            case "3102":
            case "3103":
            case "3104":
            case "3105":
            case "3106":
            case "3107":
            case "3108":
            case "3109":
            case "3110":
            case "3111":
            case "3112":
            case "3113":
            case "3114":
            case "3115":
            case "3116":
            case "3117":
            case "3118":
            case "3119":
            case "3120":
            case "3121":
            case "3122":
            case "3123":
            case "3124":
            case "3125":
            case "3126":
            case "3127":
            case "3128":
            case "3129":
            case "3130":
            case "3131":
            case "3132":
            case "3133":
            case "3134":
            case "3135":
            case "3136":
            case "3137":
            case "3138":
            case "3139":
            case "3140":
            case "3141":
            case "3142":
            case "3143":
            case "3144":
            case "3145":
            case "3146":
            case "3147":
            case "3148":
            case "3149":
            case "3150":
            case "3151":
            case "3152":
            case "3153":
            case "3154":
            case "3155":
            case "3156":
            case "3157":
            case "3158":
            case "3159":
            case "3160":
            case "3161":
            case "3162":
            case "3163":
            case "3164":
            case "3165":
            case "3166":
            case "3167":
            case "3168":
            case "3169":
            case "3170":
            case "3171":
            case "3172":
            case "3173":
            case "3174":
            case "3175":
            case "3176":
            case "3177":
            case "3178":
            case "3179":
            case "3180":
            case "3181":
            case "3182":
            case "3183":
            case "3184":
            case "3185":
            case "3186":
            case "3187":
            case "3188":
            case "3189":
            case "3190":
            case "3191":
            case "3192":
            case "3193":
            case "3194":
            case "3195":
            case "3196":
            case "3197":
            case "3198":
            case "3199":
            case "3200":
            case "3201":
            case "3202":
            case "3203":
            case "3204":
            case "3205":
            case "3206":
            case "3207":
            case "3208":
            case "3209":
            case "3210":
            case "3211":
            case "3212":
            case "3213":
            case "3214":
            case "3215":
            case "3216":
            case "3217":
            case "3218":
            case "3219":
            case "3220":
            case "3221":
            case "3222":
            case "3223":
            case "3224":
            case "3225":
            case "3226":
            case "3227":
            case "3228":
            case "3229":
            case "3230":
            case "3231":
            case "3232":
            case "3233":
            case "3234":
            case "3235":
            case "3236":
            case "3237":
            case "3238":
            case "3239":
            case "3240":
            case "3241":
            case "3242":
            case "3243":
            case "3244":
            case "3245":
            case "3246":
            case "3247":
            case "3248":
            case "3249":
            case "3250":
            case "3251":
            case "3252":
            case "3253":
            case "3254":
            case "3255":
            case "3256":
            case "3257":
            case "3258":
            case "3259":
            case "3260":
            case "3261":
            case "3262":
            case "3263":
            case "3264":
            case "3265":
            case "3266":
            case "3267":
            case "3268":
            case "3269":
            case "3270":
            case "3271":
            case "3272":
            case "3273":
            case "3274":
            case "3275":
            case "3276":
            case "3277":
            case "3278":
            case "3279":
            case "3280":
            case "3281":
            case "3282":
            case "3283":
            case "3284":
            case "3285":
            case "3286":
            case "3287":
            case "3288":
            case "3289":
            case "3290":
            case "3291":
            case "3292":
            case "3293":
            case "3294":
            case "3295":
            case "3296":
            case "3297":
            case "3298":
            case "3299":
            case "3351":
            case "3352":
            case "3353":
            case "3354":
            case "3355":
            case "3356":
            case "3357":
            case "3358":
            case "3359":
            case "3360":
            case "3361":
            case "3362":
            case "3363":
            case "3364":
            case "3365":
            case "3366":
            case "3367":
            case "3368":
            case "3369":
            case "3370":
            case "3371":
            case "3372":
            case "3373":
            case "3374":
            case "3375":
            case "3376":
            case "3377":
            case "3378":
            case "3379":
            case "3380":
            case "3381":
            case "3382":
            case "3383":
            case "3384":
            case "3385":
            case "3386":
            case "3387":
            case "3388":
            case "3389":
            case "3390":
            case "3391":
            case "3392":
            case "3393":
            case "3394":
            case "3395":
            case "3396":
            case "3397":
            case "3398":
            case "3399":
            case "3400":
            case "3401":
            case "3402":
            case "3403":
            case "3404":
            case "3405":
            case "3406":
            case "3407":
            case "3408":
            case "3409":
            case "3410":
            case "3411":
            case "3412":
            case "3413":
            case "3414":
            case "3415":
            case "3416":
            case "3417":
            case "3418":
            case "3419":
            case "3420":
            case "3421":
            case "3422":
            case "3423":
            case "3424":
            case "3425":
            case "3426":
            case "3427":
            case "3428":
            case "3429":
            case "3430":
            case "3431":
            case "3432":
            case "3433":
            case "3434":
            case "3435":
            case "3436":
            case "3437":
            case "3438":
            case "3439":
            case "3440":
            case "3441":
                transaction.category = TransactionCategory.TRANSPORT;
                break;
            case "3501":
            case "3502":
            case "3503":
            case "3504":
            case "3505":
            case "3506":
            case "3507":
            case "3508":
            case "3509":
            case "3510":
            case "3511":
            case "3512":
            case "3513":
            case "3514":
            case "3515":
            case "3516":
            case "3517":
            case "3518":
            case "3519":
            case "3520":
            case "3521":
            case "3522":
            case "3523":
            case "3524":
            case "3525":
            case "3526":
            case "3527":
            case "3528":
            case "3529":
            case "3530":
            case "3531":
            case "3532":
            case "3533":
            case "3534":
            case "3535":
            case "3536":
            case "3537":
            case "3538":
            case "3539":
            case "3540":
            case "3541":
            case "3542":
            case "3543":
            case "3544":
            case "3545":
            case "3546":
            case "3547":
            case "3548":
            case "3549":
            case "3550":
            case "3551":
            case "3552":
            case "3553":
            case "3554":
            case "3555":
            case "3556":
            case "3557":
            case "3558":
            case "3559":
            case "3560":
            case "3561":
            case "3562":
            case "3563":
            case "3564":
            case "3565":
            case "3566":
            case "3567":
            case "3568":
            case "3569":
            case "3570":
            case "3571":
            case "3572":
            case "3573":
            case "3574":
            case "3575":
            case "3576":
            case "3577":
            case "3578":
            case "3579":
            case "3580":
            case "3581":
            case "3582":
            case "3583":
            case "3584":
            case "3585":
            case "3586":
            case "3587":
            case "3588":
            case "3589":
            case "3590":
            case "3591":
            case "3592":
            case "3593":
            case "3594":
            case "3595":
            case "3596":
            case "3597":
            case "3598":
            case "3599":
            case "3600":
            case "3601":
            case "3602":
            case "3603":
            case "3604":
            case "3605":
            case "3606":
            case "3607":
            case "3608":
            case "3609":
            case "3610":
            case "3611":
            case "3612":
            case "3613":
            case "3614":
            case "3615":
            case "3616":
            case "3617":
            case "3618":
            case "3619":
            case "3620":
            case "3621":
            case "3622":
            case "3623":
            case "3624":
            case "3625":
            case "3626":
            case "3627":
            case "3628":
            case "3629":
            case "3630":
            case "3631":
            case "3632":
            case "3633":
            case "3634":
            case "3635":
            case "3636":
            case "3637":
            case "3638":
            case "3639":
            case "3640":
            case "3641":
            case "3642":
            case "3643":
            case "3644":
            case "3645":
            case "3646":
            case "3647":
            case "3648":
            case "3649":
            case "3650":
            case "3651":
            case "3652":
            case "3653":
            case "3654":
            case "3655":
            case "3656":
            case "3657":
            case "3658":
            case "3659":
            case "3660":
            case "3661":
            case "3662":
            case "3663":
            case "3664":
            case "3665":
            case "3666":
            case "3667":
            case "3668":
            case "3669":
            case "3670":
            case "3671":
            case "3672":
            case "3673":
            case "3674":
            case "3675":
            case "3676":
            case "3677":
            case "3678":
            case "3679":
            case "3680":
            case "3681":
            case "3682":
            case "3683":
            case "3684":
            case "3685":
            case "3686":
            case "3687":
            case "3688":
            case "3689":
            case "3690":
            case "3691":
            case "3692":
            case "3693":
            case "3694":
            case "3695":
            case "3696":
            case "3697":
            case "3698":
            case "3699":
            case "3700":
            case "3701":
            case "3702":
            case "3703":
            case "3704":
            case "3705":
            case "3706":
            case "3707":
            case "3708":
            case "3709":
            case "3710":
            case "3711":
            case "3712":
            case "3713":
            case "3714":
            case "3715":
            case "3716":
            case "3717":
            case "3718":
            case "3719":
            case "3720":
            case "3721":
            case "3722":
            case "3723":
            case "3724":
            case "3725":
            case "3726":
            case "3727":
            case "3728":
            case "3729":
            case "3730":
            case "3731":
            case "3732":
            case "3733":
            case "3734":
            case "3735":
            case "3736":
            case "3737":
            case "3738":
            case "3739":
            case "3740":
            case "3741":
            case "3742":
            case "3743":
            case "3744":
            case "3745":
            case "3746":
            case "3747":
            case "3748":
            case "3749":
            case "3750":
            case "3751":
            case "3752":
            case "3753":
            case "3754":
            case "3755":
            case "3756":
            case "3757":
            case "3758":
            case "3759":
            case "3760":
            case "3761":
            case "3762":
            case "3763":
            case "3764":
            case "3765":
            case "3766":
            case "3767":
            case "3768":
            case "3769":
            case "3770":
            case "3771":
            case "3772":
            case "3773":
            case "3774":
            case "3775":
            case "3776":
            case "3777":
            case "3778":
            case "3779":
            case "3780":
            case "3781":
            case "3782":
            case "3783":
            case "3784":
            case "3785":
            case "3786":
            case "3787":
            case "3788":
            case "3789":
            case "3790":
            case "3816":
            case "3835":
            case "4011":
                transaction.category = TransactionCategory.GENERAL;
                break;
            case "4111":
            case "4112":
                transaction.category = TransactionCategory.TRANSPORT;
                break;
            case "4119":
                transaction.category = TransactionCategory.GENERAL;
                break;
            case "4121":
            case "4131":
                transaction.category = TransactionCategory.TRANSPORT;
                break;
            case "4214":
            case "4215":
            case "4225":
                transaction.category = TransactionCategory.GENERAL;
                break;
            case "4411":
                transaction.category = TransactionCategory.TRANSPORT;
                break;
            case "4457":
            case "4468":
                transaction.category = TransactionCategory.ENTERTAINMENT;
                break;
            case "4511":
                transaction.category = TransactionCategory.TRANSPORT;
                break;
            case "4582":
            case "4722":
            case "4723":
                transaction.category = TransactionCategory.ENTERTAINMENT;
                break;
            case "4784":
            case "4789":
                transaction.category = TransactionCategory.TRANSPORT;
                break;
            case "4812":
                transaction.category = TransactionCategory.SHOPPING;
                break;
            case "4814":
            case "4816":
            case "4821":
            case "4829":
            case "4899":
            case "4900":
                transaction.category = TransactionCategory.BILLS_AND_HOME;
                break;
            case "5013":
                transaction.category = TransactionCategory.TRANSPORT;
                break;
            case "5021":
                transaction.category = TransactionCategory.SHOPPING;
                break;
            case "5039":
                transaction.category = TransactionCategory.BILLS_AND_HOME;
                break;
            case "5044":
            case "5045":
            case "5046":
                transaction.category = TransactionCategory.SHOPPING;
                break;
            case "5047":
                transaction.category = TransactionCategory.HEALTH_AND_BEAUTY;
                break;
            case "5051":
            case "5065":
            case "5072":
            case "5074":
            case "5085":
            case "5094":
            case "5099":
            case "5111":
                transaction.category = TransactionCategory.SHOPPING;
                break;
            case "5122":
                transaction.category = TransactionCategory.HEALTH_AND_BEAUTY;
                break;
            case "5131":
                transaction.category = TransactionCategory.SHOPPING;
                break;
            case "5137":
            case "5139":
                transaction.category = TransactionCategory.CLOTHES;
                break;
            case "5169":
                transaction.category = TransactionCategory.SHOPPING;
                break;
            case "5172":
                transaction.category = TransactionCategory.TRANSPORT;
                break;
            case "5192":
                transaction.category = TransactionCategory.ENTERTAINMENT;
                break;
            case "5193":
            case "5198":
                transaction.category = TransactionCategory.BILLS_AND_HOME;
                break;
            case "5199":
                transaction.category = TransactionCategory.SHOPPING;
                break;
            case "5200":
            case "5211":
            case "5231":
            case "5251":
            case "5261":
            case "5271":
                transaction.category = TransactionCategory.BILLS_AND_HOME;
                break;
            case "5300":
            case "5309":
            case "5310":
            case "5311":
            case "5331":
            case "5399":
                transaction.category = TransactionCategory.SHOPPING;
                break;
            case "5411":
            case "5422":
            case "5441":
            case "5451":
            case "5462":
            case "5499":
                transaction.category = TransactionCategory.GROCERIES;
                break;
            case "5511":
            case "5521":
            case "5531":
            case "5532":
            case "5533":
            case "5541":
            case "5542":
                transaction.category = TransactionCategory.TRANSPORT;
                break;
            case "5551":
            case "5561":
                transaction.category = TransactionCategory.ENTERTAINMENT;
                break;
            case "5571":
                transaction.category = TransactionCategory.TRANSPORT;
                break;
            case "5592":
                transaction.category = TransactionCategory.ENTERTAINMENT;
                break;
            case "5598":
            case "5599":
                transaction.category = TransactionCategory.TRANSPORT;
                break;
            case "5611":
            case "5621":
            case "5631":
            case "5641":
            case "5651":
            case "5655":
            case "5661":
            case "5681":
            case "5691":
            case "5697":
            case "5698":
            case "5699":
                transaction.category = TransactionCategory.CLOTHES;
                break;
            case "5712":
            case "5713":
            case "5714":
            case "5718":
            case "5719":
            case "5722":
                transaction.category = TransactionCategory.BILLS_AND_HOME;
                break;
            case "5732":
                transaction.category = TransactionCategory.SHOPPING;
                break;
            case "5733":
                transaction.category = TransactionCategory.ENTERTAINMENT;
                break;
            case "5734":
                transaction.category = TransactionCategory.SHOPPING;
                break;
            case "5735":
                transaction.category = TransactionCategory.ENTERTAINMENT;
                break;
            case "5811":
            case "5812":
            case "5813":
            case "5814":
                transaction.category = TransactionCategory.EATING_OUT;
                break;
            case "5815":
            case "5816":
                transaction.category = TransactionCategory.ENTERTAINMENT;
                break;
            case "5817":
                transaction.category = TransactionCategory.SHOPPING;
                break;
            case "5818":
                transaction.category = TransactionCategory.ENTERTAINMENT;
                break;
            case "5912":
                transaction.category = TransactionCategory.HEALTH_AND_BEAUTY;
                break;
            case "5921":
                transaction.category = TransactionCategory.GROCERIES;
                break;
            case "5931":
            case "5932":
            case "5933":
            case "5935":
            case "5937":
                transaction.category = TransactionCategory.SHOPPING;
                break;
            case "5940":
                transaction.category = TransactionCategory.TRANSPORT;
                break;
            case "5941":
            case "5942":
                transaction.category = TransactionCategory.ENTERTAINMENT;
                break;
            case "5943":
            case "5944":
                transaction.category = TransactionCategory.SHOPPING;
                break;
            case "5945":
            case "5946":
                transaction.category = TransactionCategory.ENTERTAINMENT;
                break;
            case "5947":
            case "5948":
            case "5949":
            case "5950":
                transaction.category = TransactionCategory.SHOPPING;
                break;
            case "5960":
            case "5962":
            case "5963":
            case "5964":
            case "5965":
            case "5966":
            case "5967":
            case "5968":
            case "5969":
                transaction.category = TransactionCategory.GENERAL;
                break;
            case "5970":
                transaction.category = TransactionCategory.ENTERTAINMENT;
                break;
            case "5971":
            case "5972":
            case "5973":
                transaction.category = TransactionCategory.SHOPPING;
                break;
            case "5975":
            case "5976":
            case "5977":
                transaction.category = TransactionCategory.HEALTH_AND_BEAUTY;
                break;
            case "5978":
            case "5983":
            case "5992":
            case "5993":
                transaction.category = TransactionCategory.SHOPPING;
                break;
            case "5994":
                transaction.category = TransactionCategory.ENTERTAINMENT;
                break;
            case "5995":
                transaction.category = TransactionCategory.PETS;
                break;
            case "5996":
                transaction.category = TransactionCategory.BILLS_AND_HOME;
                break;
            case "5997":
                transaction.category = TransactionCategory.SHOPPING;
                break;
            case "5998":
                transaction.category = TransactionCategory.ENTERTAINMENT;
                break;
            case "5999":
                transaction.category = TransactionCategory.SHOPPING;
                break;
            case "6010":
            case "6011":
            case "6012":
            case "6051":
            case "6211":
                transaction.category = TransactionCategory.GENERAL;
                break;
            case "6300":
                transaction.category = TransactionCategory.BILLS_AND_HOME;
                break;
            case "6513":
            case "6540":
            case "7011":
            case "7012":
                transaction.category = TransactionCategory.GENERAL;
                break;
            case "7032":
            case "7033":
                transaction.category = TransactionCategory.ENTERTAINMENT;
                break;
            case "7210":
            case "7211":
            case "7216":
                transaction.category = TransactionCategory.CLOTHES;
                break;
            case "7217":
                transaction.category = TransactionCategory.BILLS_AND_HOME;
                break;
            case "7221":
                transaction.category = TransactionCategory.ENTERTAINMENT;
                break;
            case "7230":
                transaction.category = TransactionCategory.HEALTH_AND_BEAUTY;
                break;
            case "7251":
                transaction.category = TransactionCategory.CLOTHES;
                break;
            case "7261":
            case "7273":
            case "7276":
            case "7277":
                transaction.category = TransactionCategory.GENERAL;
                break;
            case "7278":
                transaction.category = TransactionCategory.SHOPPING;
                break;
            case "7296":
                transaction.category = TransactionCategory.CLOTHES;
                break;
            case "7297":
            case "7298":
                transaction.category = TransactionCategory.HEALTH_AND_BEAUTY;
                break;
            case "7299":
            case "7311":
            case "7321":
            case "7333":
            case "7338":
            case "7339":
                transaction.category = TransactionCategory.GENERAL;
                break;
            case "7342":
            case "7349":
                transaction.category = TransactionCategory.BILLS_AND_HOME;
                break;
            case "7361":
            case "7372":
            case "7375":
            case "7379":
            case "7392":
            case "7393":
                transaction.category = TransactionCategory.GENERAL;
                break;
            case "7394":
                transaction.category = TransactionCategory.BILLS_AND_HOME;
                break;
            case "7395":
                transaction.category = TransactionCategory.ENTERTAINMENT;
                break;
            case "7399":
                transaction.category = TransactionCategory.GENERAL;
                break;
            case "7512":
            case "7513":
                transaction.category = TransactionCategory.TRANSPORT;
                break;
            case "7519":
                transaction.category = TransactionCategory.ENTERTAINMENT;
                break;
            case "7523":
            case "7531":
            case "7534":
            case "7535":
            case "7538":
            case "7542":
            case "7549":
                transaction.category = TransactionCategory.TRANSPORT;
                break;
            case "7622":
            case "7623":
            case "7629":
            case "7631":
                transaction.category = TransactionCategory.GENERAL;
                break;
            case "7641":
                transaction.category = TransactionCategory.BILLS_AND_HOME;
                break;
            case "7692":
            case "7699":
            case "7800":
            case "7801":
            case "7802":
            case "7829":
                transaction.category = TransactionCategory.GENERAL;
                break;
            case "7832":
            case "7841":
            case "7911":
            case "7922":
            case "7929":
            case "7932":
            case "7933":
            case "7941":
            case "7991":
            case "7992":
                transaction.category = TransactionCategory.ENTERTAINMENT;
                break;
            case "7993":
                transaction.category = TransactionCategory.GENERAL;
                break;
            case "7994":
                transaction.category = TransactionCategory.ENTERTAINMENT;
                break;
            case "7995":
                transaction.category = TransactionCategory.GENERAL;
                break;
            case "7996":
            case "7997":
            case "7998":
            case "7999":
                transaction.category = TransactionCategory.ENTERTAINMENT;
                break;
            case "8011":
            case "8021":
            case "8031":
            case "8041":
            case "8042":
            case "8043":
            case "8044":
            case "8049":
            case "8050":
            case "8062":
            case "8071":
            case "8099":
                transaction.category = TransactionCategory.HEALTH_AND_BEAUTY;
                break;
            case "8111":
            case "8211":
            case "8220":
            case "8241":
            case "8244":
            case "8249":
            case "8299":
            case "8351":
                transaction.category = TransactionCategory.GENERAL;
                break;
            case "8398":
            case "8641":
            case "8651":
            case "8661":
                transaction.category = TransactionCategory.CHARITY;
                break;
            case "8675":
                transaction.category = TransactionCategory.TRANSPORT;
                break;
            case "8699":
            case "8734":
            case "8911":
            case "8931":
            case "8999":
            case "9211":
            case "9222":
            case "9223":
            case "9311":
            case "9399":
            case "9402":
            case "9405":
            case "9702":
            case "9950":
                transaction.category = TransactionCategory.GENERAL;
                break;
            default:
                transaction.category = TransactionCategory.UNKNOWN;
                break;
        }

        return transaction;
    }
}
